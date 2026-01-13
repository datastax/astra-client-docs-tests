import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.core.query.Filter;
import com.datastax.astra.client.tables.Table;
import com.datastax.astra.client.tables.definition.rows.Row;

public class Example {

    public static void main(String[] args) {
        // Get an existing table
        Table<Row> table =
            new DataAPIClient("**APPLICATION_TOKEN**")
                .getDatabase("**API_ENDPOINT**")
                .getTable("**TABLE_NAME**");

        // Delete rows
        table.deleteMany(new Filter());
    }
}
