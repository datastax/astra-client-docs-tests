package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.docs.core.runner.Placeholders;
import lombok.val;

import java.util.List;
import java.util.Optional;

public class TableIndexNamesSource extends NamesSource {
    protected final Optional<String> overrideName;

    public TableIndexNamesSource(String name, WithNameAndKeyspace.TableImpl nameAndKeyspace) {
        super(name, nameAndKeyspace);
        this.overrideName = nameAndKeyspace.name();
    }

    @Override
    public List<String> names(Database db, Placeholders placeholders) {
        val tableName = overrideName
            .or(placeholders::tableName)
            .orElseThrow(() -> new PlanException("Could not determine table name from fixture metadata or override for source: " + name));

        return db.getTable(tableName).listIndexesNames();
    }
}
