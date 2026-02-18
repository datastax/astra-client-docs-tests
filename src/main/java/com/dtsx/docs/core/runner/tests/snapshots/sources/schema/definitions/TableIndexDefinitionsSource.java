package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.datastax.astra.client.tables.definition.indexes.TableIndexDescriptor;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.snapshot.meta.TableIndexDefinitionSourceMeta;
import com.dtsx.docs.core.runner.PlaceholderResolver;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;

public class TableIndexDefinitionsSource extends SnapshotSource {
    private final Set<String> indexes;
    protected final Optional<String> overrideKeyspace;
    protected final Optional<String> overrideName;

    private static final Set<String> DEFAULT_INDEXES = Set.of("INDEX_NAME");

    public TableIndexDefinitionsSource(String name, TableIndexDefinitionSourceMeta meta) {
        super(name);
        this.indexes = meta.indexes().orElse(DEFAULT_INDEXES);
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

        val shouldIncludeIndex = shouldIncludeIndex(driver);

        val indexes = table.listIndexes().stream()
            .filter(shouldIncludeIndex)
            .map(i -> i.name("example_index_name"))
            .toList();
        
        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(indexes)
        );
    }

    @SuppressWarnings("rawtypes")
    private Predicate<TableIndexDescriptor> shouldIncludeIndex(ClientDriver driver) {
        val lang = driver.language();

        val placeholderIndexName = indexes.contains("INDEX_NAME")
            ? PlaceholderResolver.mkIndexName(lang)
            : null;

        return (index) -> {
            return indexes.contains(index.getName()) || index.getName().equals(placeholderIndexName);
        };
    }
}
