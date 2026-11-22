import java.util.Map;

public class Sample {
    private final String id;
    private final String batch_id;
    private final String number;
    public Sample(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.batch_id = String.valueOf(row.get("batch_id"));
        this.number = String.valueOf(row.get("number"));
    }
    public String getId() { return id; }
    public String getBatchId() { return batch_id; }
    public String getNumber() { return number; }
}
