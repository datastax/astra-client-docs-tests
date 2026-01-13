import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.commands.options.CollectionFindOptions;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.query.Filter;
import com.datastax.astra.client.core.query.Filters;
import com.datastax.astra.client.core.query.Projection;

public class Example {

    public static void main(String[] args) {
        // Get an existing collection
        Collection<Document> collection =
            new DataAPIClient("**APPLICATION_TOKEN**")
                .getDatabase("**API_ENDPOINT**")
                .getCollection("**COLLECTION_NAME**");

        // Use a projection
        Filter filter = Filters.eq("metadata.language", "English");
        CollectionFindOptions options =
            new CollectionFindOptions()
                .projection(new Projection("metadata.edition", true), new Projection("title", true));
        collection.find(filter, options);
    }
}
