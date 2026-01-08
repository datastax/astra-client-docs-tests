package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import lombok.val;

public class AllTestsReporter extends TestReporter {
    public AllTestsReporter(VerifierCtx ctx) {
        super(ctx);
    }

    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        printFixtureHeading(history.unwrap().size(), baseFixture);
    }

    @Override
    public void printTestRootResults(JSFixture baseFixture, TestRootResults results, TestResults history) {
        printTestRootResults(results);
    }
}
