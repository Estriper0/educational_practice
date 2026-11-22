import java.util.Map;

public class PlanRequirement {
    private final String plan_id;
    private final String requirement_id;
    private final double lower;
    private final double upper;
    private final String unit;
    public PlanRequirement(Map<String,Object> row) {
        this.plan_id = String.valueOf(row.get("plan_id"));
        this.requirement_id = String.valueOf(row.get("requirement_id"));
        this.lower = ((Number) row.get("lower")).doubleValue();
        this.upper = ((Number) row.get("upper")).doubleValue();
        this.unit = String.valueOf(row.get("unit"));
    }
    public String getPlanId() { return plan_id; }
    public String getRequirementId() { return requirement_id; }
    public double getLower() { return lower; }
    public double getUpper() { return upper; }
    public String getUnit() { return unit; }
}
