package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithKeyspace;

import java.util.List;

public class TableNamesSource extends NamesSource {
    public TableNamesSource(String name, WithKeyspace.Impl keyspace) {
        super(name, keyspace);
    }

    @Override
    public List<String> names(Database db) {
        return db.listTableNames();
    }
}
