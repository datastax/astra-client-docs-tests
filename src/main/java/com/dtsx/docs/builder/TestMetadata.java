package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.TestFixture;
import com.dtsx.docs.runner.Snapshotter;

import java.nio.file.Path;
import java.util.Optional;
import java.util.TreeSet;

public record TestMetadata(
    Path exampleFolder,
    Path exampleFile,
    Optional<TestFixture> testFixture,
    TreeSet<Snapshotter> snapshotters,
    boolean shareSnapshots
) {}
