import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.commands.Update;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.query.Filters;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class Example {

    public static void main(String[] args) {
        // Get an existing collection
        Collection<Document> collection =
            new DataAPIClient("**APPLICATION_TOKEN**")
                .getDatabase("**API_ENDPOINT**")
                .getCollection("**COLLECTION_NAME**");

        Calendar calendar = Calendar.getInstance();

        // Use date in insertions
        collection.insertOne(new Document().append("registered_at", calendar));
        collection.insertOne(new Document().append("date_of_birth", new Date()));
        collection.insertOne(new Document().append("just_a_date", Instant.now()));

        // Use date in a filter
        collection.updateOne(
            Filters.eq("registered_at", calendar), Update.create().set("message", "happy Sunday!"));
        collection.findOne(
            Filters.lt("date_of_birth", new Date(System.currentTimeMillis() - 1000 * 1000)));
    }
}