package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.commands.Command;
import com.datastax.astra.client.tables.commands.options.ListTypesOptions;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.meta.UdtDefinitionSourceMeta;
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

public class UdtDefinitionsSource extends SnapshotSource {
    private final List<String> types;
    protected final Optional<String> overrideKeyspace;

    public UdtDefinitionsSource(String name, UdtDefinitionSourceMeta meta) {
        super(name);
        this.types = meta.types();
        this.overrideKeyspace = meta.keyspace();
    }

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        val database = DataAPIUtils.getDatabase(
            ctx.connectionInfo(),
            overrideKeyspace.orElse(placeholders.keyspaceName())
        );

        val listTypesCommand = Command
            .create("listTypes")
            .withOptions(new Document().append("explain", true));

        record TypeDescriptor(String udtName, Object definition) {}

        val types = database.runCommand(listTypesCommand, (ListTypesOptions) null)
            .getStatusKeyAsList("types", TypeDescriptor.class)
            .stream()
            .filter(type -> this.types.contains(type.udtName))
            .toList();
        
        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(types)
        );
    }
}
