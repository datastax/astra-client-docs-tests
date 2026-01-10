package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.snapshots.SnapshotSource;
import lombok.Getter;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.TreeSet;

/// Represents a test root containing test files for different client languages, represented by a `meta.yml` file.
///
/// Example:
/// ```
/// examples/
///  dates/            <- test root
///    example.sh
///    meta.yml
///  delete-many/
///    with-filter/    <- test root
///      java/src/main/java/Example.java
///      example.ts
///      meta.yml
///    without-filter/ <- test root
///      example.go
///      example.py
///      meta.yml
/// ```
///
/// Test roots may be nested for organizational purposes; it's simply the presence of a `meta.yml` file that defines a test root.
///
/// Each test root contains different client variants of the same example that share the same fixture and snapshot configuration, with test files called `example.<ext>`.
///
/// @see MetaYml
@Getter
public class TestRoot {
    /// The path to this test root.
    private final Path path;

    /// The different `example.<ext>` files to test within this test root, keyed by client language.
    private final TreeMap<ClientLanguage, Path> filesToTest;

    ///  The test-specific fixture to use for this test root.
    private final JSFixture testFixture;

    /// The set of snapshot sources configured for this test root.
    private final TreeSet<SnapshotSource> snapshotSources;

    /// Whether snapshots are shared across all client languages within this test root.
    ///
    /// If true, a snapshot created by one client language can be used to verify the output of another
    ///  client language within the same test root for stronger consistency and reduced effort.
    private final boolean shareSnapshots;

    /// The name of this test root relative to the examples folder.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   dates/                    -> "dates"
    ///   delete-many/with-filter/  -> "delete-many/with-filter"
    /// ```
    private final String rootName;

    public TestRoot(VerifierCtx ctx, Path path, TreeMap<ClientLanguage, Path> filesToTes, JSFixture testFixture, TreeSet<SnapshotSource> snapshotSources, boolean shareSnapshots) {
        this.path = path;
        this.filesToTest = filesToTes;
        this.testFixture = testFixture;
        this.snapshotSources = snapshotSources;
        this.shareSnapshots = shareSnapshots;
        this.rootName = ctx.examplesFolder().relativize(path).toString();
    }

    /// Returns the relative path of the example file for the specified client language.
    ///
    /// Example:
    /// ```
    /// dates/
    ///   example.ts                       -> "example.ts"
    ///   java/src/main/java/Example.java  -> "java/src/main/java/Example.java"
    /// ```
    ///
    /// @param lang the client language for which to get the example file path
    /// @return the relative path from this test root to the example file
    public String relativeExampleFilePath(ClientLanguage lang) {
        return path.relativize(filesToTest.get(lang)).toString();
    }
}
