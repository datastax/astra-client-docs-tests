package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

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

        val keyspace = overrideKeyspace.orElse(placeholders.keyspaceName());

        // Get all type names and definitions - they should be in the same order
        val allTypeNames = database.listTypeNames();
        val allTypeDescriptors = database.listTypes();

        // Match names with descriptors by index
        val typeDefinitions = new java.util.ArrayList<>();
        for (int i = 0; i < allTypeNames.size() && i < allTypeDescriptors.size(); i++) {
            val typeName = allTypeNames.get(i);
            if (this.types.contains(typeName)) {
                val descriptor = allTypeDescriptors.get(i);
                // Create a combined object with name and definition
                typeDefinitions.add(java.util.Map.of(
                    "name", typeName,
                    "definition", descriptor
                ));
            }
        }

        return JacksonUtils.prettyPrintJson(
            mkJsonDeterministic(typeDefinitions)
        );
    }
}
