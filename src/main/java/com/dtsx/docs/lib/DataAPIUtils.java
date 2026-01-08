package com.dtsx.docs.lib;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.options.DataAPIClientOptions;
import com.datastax.astra.client.databases.Database;
import com.datastax.astra.client.tables.Table;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.docs.config.ConnectionInfo;
import com.dtsx.docs.config.VerifierCtx;

import static com.dtsx.docs.lib.Constants.TEST_COLLECTION_NAME;
import static com.dtsx.docs.lib.Constants.TEST_TABLE_NAME;

public class DataAPIUtils {
    public static Collection<Document> getCollection(ConnectionInfo info) {
        return mkDb(info).getCollection(TEST_COLLECTION_NAME);
    }

    public static Table<Row> getTable(ConnectionInfo info) {
        return mkDb(info).getTable(TEST_TABLE_NAME);
    }

    private static DataAPIClient mkClient(ConnectionInfo info) {
        return new DataAPIClient(info.token(), new DataAPIClientOptions().destination(info.destination()));
    }

    private static Database mkDb(ConnectionInfo info) {
        return mkClient(info).getDatabase(info.endpoint());
    }
}
