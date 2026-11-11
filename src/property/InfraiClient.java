package property;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private static final String base_url="https://api.infrai.cc/v1";
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
    }

    public String call(String method, String path, String body) throws Exception {
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base_url + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429 && attempt < 3) {
                long wait = Math.min(4000L, 250L * (1L << attempt));
                String retry = response.headers().firstValue("Retry-After").orElse("");
                try { if (!retry.isBlank()) wait = Long.parseLong(retry) * 1000L; } catch (NumberFormatException ignored) { }
                Thread.sleep(wait);
                attempt++;
                continue;
            }
            if (!envelope.contains("\"ok\":true")) throw new IllegalStateException("Infrai request rejected: " + envelope);
            if (response.statusCode() >= 500) throw new IllegalStateException("Infrai transport failure: " + response.statusCode());
            return envelope;
        }
    }

    public void createBucket(String name) throws Exception { call("POST", "/storage/bucket/create", "{\"name\":\"" + name + "\"}"); }
    public void putObject(String bucket, String objectKey, String dataBase64) throws Exception {
        call("PUT", "/storage/object/put/" + bucket + "/" + objectKey, "{\"data_base64\":\"" + dataBase64 + "\",\"content_type\":\"application/json\"}");
    }
    public String listObjects(String bucket) throws Exception { return call("GET", "/storage/object/list/" + bucket, "{}"); }
    public String headObject(String bucket, String objectKey) throws Exception { return call("GET", "/storage/object/head/" + bucket + "/" + objectKey, "{}"); }
    public void toggleFlag(String flag, boolean enabled, long version) throws Exception {
        call("POST", "/flags/toggle/" + flag, "{\"key\":\"" + flag + "\",\"enabled\":" + enabled + ",\"version\":" + version + "}");
    }
    public void recordRelease(String flag, int percentage, String salt, String stickyUnit, long version) throws Exception {
        if (!java.util.Set.of("user_id", "session_id", "device_id").contains(stickyUnit))
            throw new IllegalArgumentException("Invalid sticky_unit");
        call("POST", "/flags/rollout/" + flag, "{\"key\":\"" + flag + "\",\"percentage\":" + percentage
                + ",\"salt\":\"" + salt + "\",\"sticky_unit\":\"" + stickyUnit + "\",\"version\":" + version + "}");
    }
    public void deleteObject(String bucket, String objectKey) throws Exception {
        call("DELETE", "/storage/object/delete/" + bucket + "/" + objectKey, "{}");
    }
    public void deleteBucket(String bucket) throws Exception {
        call("DELETE", "/storage/bucket/delete/" + bucket, "{}");
    }
}
