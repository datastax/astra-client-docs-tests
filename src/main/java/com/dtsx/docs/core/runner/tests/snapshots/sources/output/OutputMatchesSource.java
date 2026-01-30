package com.dtsx.docs.core.runner.tests.snapshots.sources.output;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.sources.OutputMatchesSourceMeta;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.val;

import java.util.regex.Pattern;

public class OutputMatchesSource extends SnapshotSource {
    protected final Pattern regex;

    public OutputMatchesSource(String name, OutputMatchesSourceMeta meta) {
        super(name);
        this.regex = meta.regex();
    }

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        val output = SnapshotSourceUtils.extractOutput(name, res);

        if (regex.matcher(output).matches()) {
            return "Matches '" + regex.pattern() + "'";
        } else {
            return "Failed to match '" + regex.pattern() + "':\n\n" + output;
        }
    }
}
