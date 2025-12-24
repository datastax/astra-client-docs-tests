package com.dtsx.docs;

import com.dtsx.docs.builder.TestPlanBuilder;
import com.dtsx.docs.runner.TestRunner;
import lombok.val;

public class Verifier {
    public static void main(String[] args) {
        val cfg = VerifierConfig.load();

        val plan = TestPlanBuilder.buildPlan(cfg);

        TestRunner.runTests(cfg, plan);
    }
}
