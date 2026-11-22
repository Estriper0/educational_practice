import java.util.Map;

public class Employee {
    private final String id;
    private final String name;
    private final String role;
    public Employee(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.name = String.valueOf(row.get("name"));
        this.role = String.valueOf(row.get("role"));
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean canApprove() { return false; }
}
