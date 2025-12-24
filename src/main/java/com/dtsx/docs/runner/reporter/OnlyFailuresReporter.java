package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class OnlyFailuresReporter extends TestReporter {
    private int printedFixtures = 0;
    private @Nullable Runnable printBaseFixtureName;

    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        printBaseFixtureName = () -> printFixtureHeading(++printedFixtures, baseFixture);
    }

    @Override
    public void printTestResult(JSFixture baseFixture, TestResult result, TestResults history) {
        if (result.approved()) {
            return;
        }

        if (printBaseFixtureName != null) {
            printBaseFixtureName.run();
            printBaseFixtureName = null;
        }

        System.out.println("- (FAILED) " + result.snapshotFile());
    }
}
