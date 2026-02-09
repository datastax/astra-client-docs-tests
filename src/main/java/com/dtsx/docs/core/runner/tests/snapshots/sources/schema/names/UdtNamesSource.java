package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithKeyspace;
import com.dtsx.docs.core.runner.Placeholders;

import java.util.List;

public class UdtNamesSource extends NamesSource {
    public UdtNamesSource(String name, WithKeyspace.Impl keyspace) {
        super(name, keyspace);
    }

    @Override
    public List<String> names(Database db, Placeholders placeholders) {
        return db.listTypeNames();
    }
}
