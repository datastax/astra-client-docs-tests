package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.Optional;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;

public abstract class SchemaObjectDefinitionSource extends SnapshotSource {
    protected final Optional<String> overrideName;
    protected final Optional<String> overrideKeyspace;

    public SchemaObjectDefinitionSource(String name, WithNameAndKeyspace nameAndKeyspace) {
        super(name);
        this.overrideName = nameAndKeyspace.name();
        this.overrideKeyspace = nameAndKeyspace.keyspace();
    }

    protected abstract Optional<String> extractSchemaObjectName(Placeholders placeholders);
    protected abstract Object getDefinition(TestCtx ctx, String name, String keyspace);

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        // error should never be thrown since it would've been caught earlier in PlaceholderResolver.resolvePlaceholders
        // since the snapshot shouldn't be depending on a collection/table that the example file doesn't explicitly use anyway
        val schemaObjName = overrideName
            .or(() -> extractSchemaObjectName(placeholders))
            .orElseThrow(() -> new PlanException("Could not determine schema object name from fixture metadata or override for source: " + name));

        val schemaObjKeyspace = overrideKeyspace.orElse(placeholders.keyspaceName());

        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(getDefinition(ctx, schemaObjName, schemaObjKeyspace))
        );
    }
}
