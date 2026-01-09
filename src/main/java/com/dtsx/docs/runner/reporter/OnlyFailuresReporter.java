package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import org.jetbrains.annotations.Nullable;

/// Reporter that only shows failed tests, hiding passing ones for cleaner output.
///
/// Fixture headings are only printed if they contain at least one failure.
/// This keeps output minimal when most tests pass.
///
/// Output format (only failures shown):
/// ```
/// 1) ✗ basic-collection.js
///   - find-many
///     - ✓ example.ts
///     - ✗ example.py
///     - ✓ example.java
/// ```
public class OnlyFailuresReporter extends TestReporter {
    private int printedFixtures = 0;
    private @Nullable Runnable printBaseFixtureName;

    public OnlyFailuresReporter(VerifierCtx ctx) {
        super(ctx);
    }

    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        // Defer printing until we know there's a failure in this fixture group
        printBaseFixtureName = () -> printFixtureHeading(++printedFixtures, baseFixture);
    }

    @Override
    public void printTestRootResults(JSFixture baseFixture, TestRootResults results, TestResults history) {
        // Skip if all tests passed
        if (results.allPassed()) {
            return;
        }

        // Print the fixture heading now that we know there's a failure
        if (printBaseFixtureName != null) {
            printBaseFixtureName.run();
            printBaseFixtureName = null;
        }

        printTestRootResults(results);
    }
}