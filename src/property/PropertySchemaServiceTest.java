package property;

public final class PropertySchemaServiceTest {
    public static void main(String[] args) {
        PropertySchemaService service = new PropertySchemaService();
        var request = new PropertySchemaService.MaintenanceRequest("mr-1", "tenant-1", "Leaking tap");
        var oldPath = service.decide(false, request);
        var newPath = service.decide(true, request);
        if (oldPath.useV2() || !oldPath.route().equals("maintenance_requests")) throw new AssertionError("flag-off rollback failed");
        if (!newPath.useV2() || !newPath.route().equals("maintenance_requests_v2")) throw new AssertionError("flag-on path failed");
        System.out.println("schema decision test passed");
    }
}
