package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.runner.Snapshotter;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public record TestMetadata(
    Path exampleFolder,
    List<Pair<ClientLanguage, Path>> exampleFiles,
    JSFixture testFixture,
    TreeSet<Snapshotter> snapshotters,
    boolean shareSnapshots
) {}
