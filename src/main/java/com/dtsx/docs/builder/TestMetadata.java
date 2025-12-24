package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.runner.Snapshotter;

import java.nio.file.Path;
import java.util.TreeSet;

public record TestMetadata(
    Path exampleFolder,
    Path exampleFile,
    JSFixture testFixture,
    TreeSet<Snapshotter> snapshotters,
    boolean shareSnapshots
) {}
