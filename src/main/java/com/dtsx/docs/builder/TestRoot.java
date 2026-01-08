package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.snapshots.SnapshotSource;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.TreeSet;

public record TestRoot(
    Path path,
    TreeMap<ClientLanguage, Path> filesToTest,
    JSFixture testFixture,
    TreeSet<SnapshotSource> snapshotters,
    boolean shareSnapshots
) {
    public String rootName(VerifierCtx ctx) {
        return ctx.examplesFolder().relativize(path).toString();
    }

    public String relativeExampleFilePath(ClientLanguage lang) {
        return path.relativize(filesToTest.get(lang)).toString();
    }
}
