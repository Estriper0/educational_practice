import java.util.*;
public final class ProtocolService {
    private final Dataset data;
    public ProtocolService(Dataset data) { this.data=data; }
    /** Algorithm B: same traversal and issue order as Python, O(n*m*r). */
    public List<List<String>> evaluate(List<Sample> samples,List<PlanRequirement> rules,List<Measurement> rows) {
        List<List<String>> issues=new ArrayList<>();
        for (Sample sample:samples) for (PlanRequirement rule:rules) {
            List<Measurement> found=new ArrayList<>();
            for (Measurement row:rows)
                if (row.getSampleId().equals(sample.getId()) && row.getRequirementId().equals(rule.getRequirementId())) found.add(row);
            if(found.size()!=1) issues.add(List.of(sample.getId(),rule.getRequirementId(),"missing_or_duplicate"));
            else {
                Measurement row=found.get(0); Check check=new LimitCheck(rule);
                if (!check.test(Map.of("value",row.getValue(),"unit",row.getUnit())))
                    issues.add(List.of(sample.getId(),rule.getRequirementId(),"nonconformity"));
            }
        }
        return issues;
    }
    public Map<String,Object> makeProtocol(String batchId) {
        Batch batch=data.objects("batches",Batch.class).stream().filter(b->b.getId().equals(batchId)).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Batch not found"));
        var samples=data.objects("samples",Sample.class).stream().filter(s->s.getBatchId().equals(batchId)).toList();
        var rules=data.objects("plan_requirements",PlanRequirement.class).stream().filter(p->p.getPlanId().equals(batch.getPlanId())).toList();
        Set<String> ids=new HashSet<>(); for (Sample s:samples) ids.add(s.getId());
        var rows=data.objects("measurements",Measurement.class).stream().filter(m->ids.contains(m.getSampleId())).toList();
        var issues=evaluate(samples,rules,rows);
        boolean complete=!samples.isEmpty()&&!rules.isEmpty()&&issues.stream().noneMatch(i->i.get(2).equals("missing_or_duplicate"));
        String proposal=!complete ? "incomplete" : issues.isEmpty() ? "accepted" : "rejected";
        var approval=data.rows("protocols").stream().filter(p->p.get("batch_id").equals(batchId)).findFirst().orElse(null);
        if(approval!=null && (proposal.equals("incomplete") || !proposal.equals(approval.get("decision"))))
            throw new IllegalArgumentException("Approval contradicts measurements");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("batch_id",batchId); result.put("proposal",proposal); result.put("issues",issues);
        result.put("status",approval==null ? "awaiting_approval" : approval.get("decision")); result.put("approval",approval);
        return result;
    }
}
