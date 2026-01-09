package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.snapshots.SnapshotSource;

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
public record TestRoot(
    Path path,
    TreeMap<ClientLanguage, Path> filesToTest,
    JSFixture testFixture,
    TreeSet<SnapshotSource> snapshotters,
    boolean shareSnapshots
) {
    /// Returns the name of this test root relative to the examples folder.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   dates/                    -> "dates"
    ///   delete-many/with-filter/  -> "delete-many/with-filter"
    /// ```
    ///
    /// @param ctx the verifier context containing the examples folder path
    /// @return the relative name of this test root
    public String rootName(VerifierCtx ctx) {
        return ctx.examplesFolder().relativize(path).toString();
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
