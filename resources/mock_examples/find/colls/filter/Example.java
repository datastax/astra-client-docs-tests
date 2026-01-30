import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.commands.cursor.CollectionFindCursor;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.query.Filter;
import com.datastax.astra.client.core.query.Filters;

public class Example {

    public static void main(String[] args) {
        // Get an existing collection
        Collection<Document> collection =
            new DataAPIClient("**APPLICATION_TOKEN**")
                .getDatabase("**API_ENDPOINT**")
                .getCollection("**COLLECTION_NAME**");

        // Find documents
        Filter filter =
            Filters.and(Filters.eq("is_checked_out", false), Filters.lt("number_of_pages", 300));
        CollectionFindCursor<Document, Document> cursor = collection.find(filter);

        // Iterate over the found documents
        for (Document document : cursor) {
            System.out.println(document);
        }
    }
}