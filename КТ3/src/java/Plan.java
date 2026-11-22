import java.util.Map;

public class Plan {
    private final String id;
    private final String name;
    private final String revision;
    private final String document;
    public Plan(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.name = String.valueOf(row.get("name"));
        this.revision = String.valueOf(row.get("revision"));
        this.document = String.valueOf(row.get("document"));
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRevision() { return revision; }
    public String getDocument() { return document; }
}
