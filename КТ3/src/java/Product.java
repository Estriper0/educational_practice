import java.util.Map;

public class Product {
    private final String id;
    private final String name;
    public Product(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.name = String.valueOf(row.get("name"));
    }
    public String getId() { return id; }
    public String getName() { return name; }
}
