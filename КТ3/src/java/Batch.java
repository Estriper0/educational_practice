import java.util.Map;

public class Batch {
    private final String id;
    private final String product_id;
    private final String plan_id;
    private final String number;
    public Batch(Map<String,Object> row) {
        this.id = String.valueOf(row.get("id"));
        this.product_id = String.valueOf(row.get("product_id"));
        this.plan_id = String.valueOf(row.get("plan_id"));
        this.number = String.valueOf(row.get("number"));
    }
    public String getId() { return id; }
    public String getProductId() { return product_id; }
    public String getPlanId() { return plan_id; }
    public String getNumber() { return number; }
}
