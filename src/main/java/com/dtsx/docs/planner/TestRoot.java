package com.dtsx.docs.planner;

import com.dtsx.docs.planner.meta.reps.SnapshotTestMetaYmlRep;
import com.dtsx.docs.runner.tests.strategies.CompilesTestStrategy;
import com.dtsx.docs.runner.tests.strategies.SnapshotTestStrategy;
import com.dtsx.docs.runner.tests.strategies.TestStrategy;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.runner.RunException;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeMap;

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
/// @see SnapshotTestMetaYmlRep
@Getter
public class TestRoot {
    /// The path to this test root.
    private final Path path;

    /// The different `example.<ext>` files to test within this test root, keyed by client language.
    private final TreeMap<ClientLanguage, Set<Path>> filesToTest;

    /// The test strategy to use for this test root.
    ///
    /// @see SnapshotTestStrategy
    /// @see CompilesTestStrategy
    private final TestStrategy testStrategy;

    /// The name of this test root relative to the examples folder.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   dates/                    -> "dates"
    ///   delete-many/with-filter/  -> "delete-many/with-filter"
    /// ```
    private final String rootName;

    public TestRoot(TestCtx ctx, Path path, TreeMap<ClientLanguage, Set<Path>> filesToTest, TestStrategy testStrategy) {
        this.path = path;
        this.filesToTest = filesToTest;
        this.testStrategy = testStrategy;
        this.rootName = ctx.examplesFolder().relativize(path).toString();
    }

    /// Returns the relative path of the example file for the specified file to test from this test root.
    ///
    /// Example:
    /// ```
    /// dates/
    ///   example.ts                       -> "example.ts"
    ///   java/src/main/java/Example.java  -> "java/src/main/java/Example.java"
    /// ```
    ///
    /// @param fileToTest the path to the example file to test
    /// @return the relative path from this test root to the example file
    public String displayPath(Path fileToTest) {
        if (!fileToTest.startsWith(this.path)) {
            throw new RunException("File to test is not within the test root path"); // sanity check; should never be thrown
        }
        return rootName + "/" + path.relativize(fileToTest);
    }
}
