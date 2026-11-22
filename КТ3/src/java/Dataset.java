import java.util.*;
/** JSON boundary. Checks integrity before materializing domain objects. */
public final class Dataset {
    private final Map<String,List<Map<String,Object>>> tables = new LinkedHashMap<>();
    private final Map<String,Map<String,Map<String,Object>>> indexes = new LinkedHashMap<>();
    private final Map<String,List<Object>> entities = new LinkedHashMap<>();
    @SuppressWarnings("unchecked")
    public Dataset(Object input) {
        if (!(input instanceof Map<?,?> root)) throw new IllegalArgumentException("Expected object");
        for (String table : List.of("products","plans","requirements","plan_requirements","batches","samples","measurements","employees","protocols")) {
            if (!(root.get(table) instanceof List<?> list)) fail("Expected list: " + table);
            List<Map<String,Object>> rows = new ArrayList<>();
            for (Object row : (List<?>)root.get(table)) {
                if (!(row instanceof Map<?,?>)) fail("Expected row: " + table);
                rows.add(Collections.unmodifiableMap(new LinkedHashMap<>((Map<String,Object>) row)));
            }
            tables.put(table, List.copyOf(rows));
        }
        Map<String,String> fields = new LinkedHashMap<>();
        fields.put("products","id name"); fields.put("plans","id name revision document");
        fields.put("requirements","id name"); fields.put("batches","id product_id plan_id number");
        fields.put("samples","id batch_id number"); fields.put("employees","id name role");
        fields.put("measurements","id sample_id requirement_id employee_id value unit measured_at");
        fields.put("protocols","id batch_id employee_id decision approved_at");
        for (var entry : fields.entrySet()) {
            Map<String,Map<String,Object>> ix = new LinkedHashMap<>();
            for (var row : rows(entry.getKey())) {
                required(row,entry.getValue()); Object id=row.get("id");
                if (!(id instanceof String) || ((String)id).isEmpty() || ix.containsKey(id)) fail("Invalid primary key");
                ix.put((String)id,row);
            }
            indexes.put(entry.getKey(),ix);
        }
        for (String spec : List.of("batches product_id products","batches plan_id plans","samples batch_id batches",
                "measurements sample_id samples","measurements requirement_id requirements","measurements employee_id employees",
                "protocols batch_id batches","protocols employee_id employees")) {
            String[] s=spec.split(" ");
            for (var row:rows(s[0])) if (!indexes.get(s[2]).containsKey(row.get(s[1]))) fail("Invalid foreign key: "+spec);
        }
        Set<List<Object>> pairs=new HashSet<>();
        for (var row:rows("plan_requirements")) {
            required(row,"plan_id requirement_id lower upper unit");
            if (!indexes.get("plans").containsKey(row.get("plan_id")) || !indexes.get("requirements").containsKey(row.get("requirement_id"))
                || !pairs.add(List.of(row.get("plan_id"),row.get("requirement_id")))) fail("Invalid plan key");
            for (String key:List.of("lower","upper"))
                if (!(row.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue())) fail("Invalid bounds");
            if (((Number)row.get("lower")).doubleValue()>((Number)row.get("upper")).doubleValue()) fail("Reversed bounds");
        }
        if (!validateMeasurements().isEmpty()) fail("Invalid measurement format");
        for (var row:rows("measurements")) {
            var sample=indexes.get("samples").get(row.get("sample_id"));
            var batch=indexes.get("batches").get(sample.get("batch_id"));
            if (!pairs.contains(List.of(batch.get("plan_id"),row.get("requirement_id")))) fail("Measurement outside plan");
        }
        Set<Object> batches=new HashSet<>();
        for (var row:rows("protocols")) {
            var employee=indexes.get("employees").get(row.get("employee_id"));
            Employee actor="approver".equals(employee.get("role")) ? new Approver(employee) : new Employee(employee);
            if (!batches.add(row.get("batch_id")) || !actor.canApprove()) fail("Invalid approver or duplicate decision");
            if (!List.of("accepted","rejected").contains(row.get("decision")) || !truthy(row.get("approved_at"))) fail("Invalid approval");
        }
        for (String table:tables.keySet()) {
            List<Object> list=new ArrayList<>();
            for (var row:rows(table)) list.add(switch(table) {
                case "products" -> new Product(row); case "plans" -> new Plan(row);
                case "requirements" -> new Requirement(row); case "plan_requirements" -> new PlanRequirement(row);
                case "batches" -> new Batch(row); case "samples" -> new Sample(row);
                case "measurements" -> new Measurement(row); case "protocols" -> new Protocol(row);
                default -> "approver".equals(row.get("role")) ? new Approver(row) : new Employee(row);
            });
            entities.put(table,List.copyOf(list));
        }
    }
    private static boolean truthy(Object v) {
        if (v==null || Boolean.FALSE.equals(v) || "".equals(v)) return false;
        if (v instanceof Number n) return n.doubleValue()!=0;
        if (v instanceof Collection<?> c) return !c.isEmpty();
        if (v instanceof Map<?,?> m) return !m.isEmpty();
        return true;
    }
    private static void required(Map<String,Object> row,String fields) {
        for (String key:fields.split(" ")) if (row.get(key)==null) fail("Missing field: "+key);
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
    public List<Map<String,Object>> rows(String table) { return tables.get(table); }
    public <T> List<T> objects(String table,Class<T> type) { return entities.get(table).stream().map(type::cast).toList(); }
    /** Algorithm A, exactly one pass, O(r). */
    public List<Integer> validateMeasurements() {
        Check check=new FormatCheck(); List<Integer> errors=new ArrayList<>();
        var rows=rows("measurements");
        for(int i=0;i<rows.size();i++) if (!check.test(rows.get(i))) errors.add(i);
        return errors;
    }
}
