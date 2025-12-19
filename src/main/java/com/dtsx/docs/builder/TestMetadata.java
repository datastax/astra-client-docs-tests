package com.dtsx.docs.builder;

import com.dtsx.docs.snapshotters.SnapshotType;

import java.nio.file.Path;
import java.util.Optional;
import java.util.TreeSet;

public record TestMetadata(
    Path exampleFolder,
    Path exampleFile,
    Optional<TestFixture> specializedFixture,
    TreeSet<SnapshotType> snapshotTypes,
    boolean shareSnapshots
) {}
