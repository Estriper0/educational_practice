import java.util.Map;
public final class FormatCheck implements Check {
    @Override public boolean test(Map<String,Object> row) {
        if (row == null) return false;
        for (String key : new String[]{"id","sample_id","requirement_id","employee_id","measured_at","unit"})
            if (row.get(key) == null) return false;
        return row.get("value") instanceof Number n && Double.isFinite(n.doubleValue());
    }
}
