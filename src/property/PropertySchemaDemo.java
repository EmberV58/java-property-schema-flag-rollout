package property;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PropertySchemaDemo {


    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(key);
        PropertySchemaService service = new PropertySchemaService();
        PropertySchemaService.MaintenanceRequest request =
                new PropertySchemaService.MaintenanceRequest("mr-104", "tenant-7", "Replace the hallway light");

        String bucket = "pd-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String objectKey = "maintenance/" + request.id() + ".json";
        client.createBucket(bucket);
        System.out.println("created bucket=" + bucket);
        try {
        String payload = "{\"request_id\":\"" + request.id() + "\",\"tenant_id\":\"" + request.tenantId() + "\",\"description\":\"" + request.description() + "\"}";
        client.putObject(bucket, "maintenance/" + request.id() + ".json", Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));
        String flag = System.getenv("INFRAI_FLAG_KEY");
        if (flag != null && !flag.isBlank()) {
            long toggleVersion = Long.parseLong(System.getenv("INFRAI_FLAG_TOGGLE_VERSION"));
            long rolloutVersion = Long.parseLong(System.getenv("INFRAI_FLAG_ROLLOUT_VERSION"));
            client.toggleFlag(flag, true, toggleVersion);
            client.recordRelease(flag, 100, "property-schema-v2", "user_id", rolloutVersion);
        } else {
            System.out.println("Flag API calls skipped; configure an existing flag and explicit versions to enable them.");
        }
        PropertySchemaService.Decision decision = service.decide(true, request);
        System.out.println("maintenance request " + request.id() + " uses " + decision.route());
        String listed = client.listObjects(bucket);
        if (!listed.contains("\"items\"")) throw new IllegalStateException("storage list did not return items");
        System.out.println("items=" + listed);
        String document = client.headObject(bucket, "maintenance/" + request.id() + ".json");
        if (document.contains("\"found\":false")) System.out.println("document is absent");
        else System.out.println("document=" + document);
        } finally {
            try {
                client.deleteObject(bucket, objectKey);
                String remaining = client.listObjects(bucket);
                if (remaining.contains(request.id() + ".json"))
                    throw new IllegalStateException("Object still listed after deletion: " + remaining);
                System.out.println("verified object deletion=" + objectKey);
            } finally {
                client.deleteBucket(bucket);
                try {
                    client.listObjects(bucket);
                    throw new IllegalStateException("Deleted bucket remains accessible: " + bucket);
                } catch (IllegalStateException expected) {
                    String message = expected.getMessage().toLowerCase(java.util.Locale.ROOT);
                    if (!(message.contains("not_found") || message.contains("not found") || message.contains("does not exist"))) throw expected;
                }
                System.out.println("verified bucket deletion=" + bucket);
            }
        }
    }
}
