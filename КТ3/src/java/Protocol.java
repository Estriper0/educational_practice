import java.util.Map;

public class Protocol {
    private final String id;
    private final String batch_id;
    private final String employee_id;
    private final String decision;
    private final String approved_at;
    public Protocol(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.batch_id = String.valueOf(row.get("batch_id"));
        this.employee_id = String.valueOf(row.get("employee_id"));
        this.decision = String.valueOf(row.get("decision"));
        this.approved_at = String.valueOf(row.get("approved_at"));
    }
    public String getId() { return id; }
    public String getBatchId() { return batch_id; }
    public String getEmployeeId() { return employee_id; }
    public String getDecision() { return decision; }
    public String getApprovedAt() { return approved_at; }
}
