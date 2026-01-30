package com.dtsx.docs.core.runner.tests.snapshots.sources.output;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;

public class OutputCaptureSource extends SnapshotSource {
    public OutputCaptureSource(String name, Void ignored) {
        super(name);
    }

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        return SnapshotSourceUtils.extractOutput(name, res);
    }
}
