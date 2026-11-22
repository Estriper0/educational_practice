import java.util.Map;

public class Requirement {
    private final String id;
    private final String name;
    public Requirement(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.name = String.valueOf(row.get("name"));
    }
    public String getId() { return id; }
    public String getName() { return name; }
}
