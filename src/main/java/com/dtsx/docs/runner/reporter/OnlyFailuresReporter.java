package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import org.jetbrains.annotations.Nullable;

public class OnlyFailuresReporter extends TestReporter {
    private int printedFixtures = 0;
    private @Nullable Runnable printBaseFixtureName;

    public OnlyFailuresReporter(VerifierCtx ctx) {
        super(ctx);
    }

    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        printBaseFixtureName = () -> printFixtureHeading(++printedFixtures, baseFixture);
    }

    @Override
    public void printTestRootResults(JSFixture baseFixture, TestRootResults results, TestResults history) {
        if (results.allApproved()) {
            return;
        }

        if (printBaseFixtureName != null) {
            printBaseFixtureName.run();
            printBaseFixtureName = null;
        }

        printTestRootResults(results);
    }
}
