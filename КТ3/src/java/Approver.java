import java.util.Map;
/** Authorized OTK employee: changes permission behavior, not just stored data. */
public final class Approver extends Employee {
    public Approver(Map<String,Object> row) { super(row); }
    @Override public boolean canApprove() { return true; }
}
