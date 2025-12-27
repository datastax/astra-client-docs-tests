package com.dtsx.docs.lib;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.tables.Table;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.docs.config.VerifierCtx;

import static com.dtsx.docs.lib.Constants.TEST_COLLECTION_NAME;
import static com.dtsx.docs.lib.Constants.TEST_TABLE_NAME;

public class DataAPIUtils {
    public static Collection<Document> getCollection(VerifierCtx ctx) {
        return new DataAPIClient(ctx.token()).getDatabase(ctx.apiEndpoint()).getCollection(TEST_COLLECTION_NAME);
    }

    public static Table<Row> getTable(VerifierCtx ctx) {
        return new DataAPIClient(ctx.token()).getDatabase(ctx.apiEndpoint()).getTable(TEST_TABLE_NAME);
    }
}
