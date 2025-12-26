package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestResult;
import lombok.val;

public class AllTestsReporter extends TestReporter {
    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        printFixtureHeading(history.unwrap().size(), baseFixture);
    }

    @Override
    public void printTestResult(JSFixture baseFixture, TestResult result, TestResults history) {
        val status = (result.approved())
            ? "PASSED"
            : "FAILED";

        CliLogger.println("  - (" + status + ") " + result.snapshotFile());
    }
}
