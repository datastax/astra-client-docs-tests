package com.dtsx.docs.lib;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.options.DataAPIClientOptions;
import com.datastax.astra.client.databases.Database;
import com.datastax.astra.client.tables.Table;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.docs.config.ConnectionInfo;

public class DataAPIUtils {
    public static Collection<Document> getCollection(ConnectionInfo info, String name) {
        return mkDb(info).getCollection(name);
    }

    public static Table<Row> getTable(ConnectionInfo info, String name) {
        return mkDb(info).getTable(name);
    }

    private static DataAPIClient mkClient(ConnectionInfo info) {
        return new DataAPIClient(info.token(), new DataAPIClientOptions().destination(info.destination()));
    }

    private static Database mkDb(ConnectionInfo info) {
        return mkClient(info).getDatabase(info.endpoint());
    }
}
