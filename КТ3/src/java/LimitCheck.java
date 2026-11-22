import java.util.Map;
public final class LimitCheck implements Check {
    private final PlanRequirement rule;
    public LimitCheck(PlanRequirement rule) { this.rule = rule; }
    @Override public boolean test(Map<String,Object> row) {
        return row.get("value") instanceof Number n
            && rule.getUnit().equals(row.get("unit"))
            && rule.getLower() <= n.doubleValue() && n.doubleValue() <= rule.getUpper();
    }
}
