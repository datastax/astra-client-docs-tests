package com.dtsx.docs.core.runner.tests.snapshots.sources.output;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.sources.OutputJsonifySourceMeta;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;

import static com.dtsx.docs.lib.JacksonUtils.runJq;

public class OutputJsonifySource extends SnapshotSource {
    private final OutputJsonifySourceMeta meta;

    public OutputJsonifySource(String name, OutputJsonifySourceMeta meta) {
        super(name);
        this.meta = meta;
    }

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        val output = SnapshotSourceUtils.extractOutput(name, res);
        var json = driver.preprocessToJson(ctx, meta, output);

        if (meta.jq().isPresent()) {
            val jsonAsString = JacksonUtils.printJson(json);
            val jqProcessed = runJq(ctx, meta.jq().get(), jsonAsString);
            json = JacksonUtils.parseJson(jqProcessed, List.class);
        }

        return JacksonUtils.prettyPrintJson(
            SnapshotSourceUtils.mkJsonDeterministic(json)
        );
    }
}
