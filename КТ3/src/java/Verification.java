import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
/** Portable regression suite, no dependencies or enabled assertions required. */
public final class Verification {
    private static Path fixture;
    @SuppressWarnings("unchecked")
    private static Map<String,Object> fresh() throws Exception { return (Map<String,Object>)Json.read(fixture); }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> rows(Map<String,Object> d,String table) { return (List<Map<String,Object>>)d.get(table); }
    private static void check(String name,Consumer<Map<String,Object>> change,String proposal,String status) throws Exception {
        var d=fresh(); change.accept(d);
        var r=new ProtocolService(new Dataset(d)).makeProtocol("B1");
        if(!r.get("proposal").equals(proposal)||!r.get("status").equals(status)) throw new AssertionError(name);
        if(!r.get("batch_id").equals("B1") || r.size()!=5) throw new AssertionError(name+": result fields");
        List<List<String>> expectedIssues=switch(name) {
            case "rejected", "unit" -> List.of(List.of("S1","R1","nonconformity"));
            case "missing", "duplicate pair" -> List.of(List.of("S1","R1","missing_or_duplicate"));
            default -> List.of();
        };
        if(!r.get("issues").equals(expectedIssues)) throw new AssertionError(name+": issue contents or order");
        Object expectedApproval=rows(d,"protocols").isEmpty()?null:rows(d,"protocols").get(0);
        if(!Objects.equals(r.get("approval"),expectedApproval)) throw new AssertionError(name+": approval");
        System.out.println("PASS "+name);
    }
    private static void invalid(String name,Consumer<Map<String,Object>> change) throws Exception {
        var d=fresh(); change.accept(d);
        try {new ProtocolService(new Dataset(d)).makeProtocol("B1");}
        catch(IllegalArgumentException e) {System.out.println("PASS "+name);return;}
        throw new AssertionError(name);
    }
    private static void approve(Map<String,Object> d,String employee,String decision) {
        rows(d,"protocols").add(new LinkedHashMap<>(Map.of("id","PR1","batch_id","B1","employee_id",employee,"decision",decision,"approved_at","2026-11-24T12:00:00+03:00")));
    }
    public static void main(String[] args) throws Exception {
        fixture=Path.of(args.length==0?"example.json":args[0]);
        check("original",d->{},"accepted","awaiting_approval");
        check("rejected",d->rows(d,"measurements").get(0).put("value",20),"rejected","awaiting_approval");
        check("missing",d->rows(d,"measurements").remove(0),"incomplete","awaiting_approval");
        check("duplicate pair",d->{var r=new LinkedHashMap<>(rows(d,"measurements").get(0));r.put("id","M5");rows(d,"measurements").add(r);},"incomplete","awaiting_approval");
        check("unit",d->rows(d,"measurements").get(0).put("unit","V"),"rejected","awaiting_approval");
        check("lower inclusive",d->rows(d,"measurements").get(0).put("value",0),"accepted","awaiting_approval");
        check("upper inclusive",d->rows(d,"measurements").get(0).put("value",10),"accepted","awaiting_approval");
        check("no samples",d->{rows(d,"samples").clear();rows(d,"measurements").clear();},"incomplete","awaiting_approval");
        check("approved",d->approve(d,"E2","accepted"),"accepted","accepted");
        invalid("unauthorized",d->approve(d,"E1","accepted"));
        invalid("contradiction",d->approve(d,"E2","rejected"));
        invalid("NaN",d->rows(d,"measurements").get(0).put("value",Double.NaN));
        invalid("boolean",d->rows(d,"measurements").get(0).put("value",true));
        invalid("foreign key",d->rows(d,"measurements").get(0).put("sample_id","unknown"));
        invalid("reversed bounds",d->rows(d,"plan_requirements").get(0).put("lower",100));
        invalid("duplicate id",d->rows(d,"products").add(new LinkedHashMap<>(rows(d,"products").get(0))));
        for(double upper:new double[]{0,Double.NaN}) {
            boolean rejected=false;
            try { new PlanRequirement(Map.of("plan_id","CP1","requirement_id","R1","lower",10,"upper",upper,"unit","Ohm")); }
            catch(IllegalArgumentException e) { rejected=true; }
            if(!rejected) throw new AssertionError("Constructor accepted invalid bounds");
        }
        System.out.println("OK: 18 scenarios");
    }
}
