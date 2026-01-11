import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.definition.documents.Document;

public class Example {

    public static void main(String[] args) {
        // Get an existing collection
        Collection<Document> collection =
            new DataAPIClient("**APPLICATION_TOKEN**")
                .getDatabase("**API_ENDPOINT**")
                .getCollection("**COLLECTION_NAME**");

        // Estimate count
        long result = collection.estimatedDocumentCount();
        System.out.println(result);
    }
}