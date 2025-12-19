package com.dtsx.docs;

import com.dtsx.docs.builder.TestsBuilder;
import com.dtsx.docs.runner.TestRunner;
import lombok.val;

public class Verifier {
    public static void main(String[] args) {
        val cfg = VerifierConfig.load();

        val tests = TestsBuilder.buildExampleTests(cfg);

        System.out.println(tests);

        TestRunner.runTests(cfg, tests);
    }
}
