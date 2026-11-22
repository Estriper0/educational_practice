import java.util.Map;

public class Measurement {
    private final String id;
    private final String sample_id;
    private final String requirement_id;
    private final String employee_id;
    private final double value;
    private final String unit;
    private final String measured_at;
    public Measurement(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.sample_id = String.valueOf(row.get("sample_id"));
        this.requirement_id = String.valueOf(row.get("requirement_id"));
        this.employee_id = String.valueOf(row.get("employee_id"));
        this.value = ((Number) row.get("value")).doubleValue();
        this.unit = String.valueOf(row.get("unit"));
        this.measured_at = String.valueOf(row.get("measured_at"));
    }
    public String getId() { return id; }
    public String getSampleId() { return sample_id; }
    public String getRequirementId() { return requirement_id; }
    public String getEmployeeId() { return employee_id; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public String getMeasuredAt() { return measured_at; }
}
