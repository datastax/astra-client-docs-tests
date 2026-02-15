package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.snapshot.meta.TableIndexDefinitionSourceMeta;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;

public class TableIndexDefinitionsSource extends SnapshotSource {
    private final List<String> indexes;
    protected final Optional<String> overrideKeyspace;
    protected final Optional<String> overrideName;

    public TableIndexDefinitionsSource(String name, TableIndexDefinitionSourceMeta meta) {
        super(name);
        this.indexes = meta.indexes();
        this.overrideKeyspace = meta.keyspace();
        this.overrideName = meta.name();
    }

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        val tableName = overrideName
            .or(placeholders::tableName)
            .orElseThrow(() -> new PlanException("Could not determine table name from fixture metadata or override for source: " + name));

        val table = DataAPIUtils.getTable(
            ctx.connectionInfo(),
            tableName,
            overrideKeyspace.orElse(placeholders.keyspaceName())
        );

        val indexes = table.listIndexes().stream()
            .filter(index -> this.indexes.contains(index.getName()))
            .toList();
        
        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(indexes)
        );
    }
}
