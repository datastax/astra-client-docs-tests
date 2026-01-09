import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.commands.results.CollectionDeleteResult;
import com.datastax.astra.client.collections.definition.documents.Document;

public class Example {

    public static void main(String[] args) {
        // Get an existing collection
        Collection<Document> collection =
            new DataAPIClient("**APPLICATION_TOKEN**")
                .getDatabase("**API_ENDPOINT**")
                .getCollection("**COLLECTION_NAME**");

        // Delete documents
        CollectionDeleteResult result = collection.deleteMany(null);
        System.out.println(result.getDeletedCount());
    }
}
