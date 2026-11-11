package property;

public final class PropertySchemaService {
    public record MaintenanceRequest(String id, String tenantId, String description) {}
    public record TenantDocument(String tenantId, String name) {}
    public record InspectionReminder(String propertyId, String dueDate) {}
    public record Decision(boolean useV2, String route) {}

    public Decision decide(boolean flagEnabled, MaintenanceRequest request) {
        if (request == null || request.description() == null || request.description().isBlank())
            throw new IllegalArgumentException("description is required");
        return flagEnabled ? new Decision(true, "maintenance_requests_v2") : new Decision(false, "maintenance_requests");
    }
}
