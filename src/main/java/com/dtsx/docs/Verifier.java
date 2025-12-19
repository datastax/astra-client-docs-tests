package com.dtsx.docs;

import com.dtsx.docs.builder.TestsBuilder;
import com.dtsx.docs.runner.TestsRunner;
import lombok.val;

public class Verifier {
    public static void main(String[] args) {
        val cfg = VerifierConfig.load();

        val tests = TestsBuilder.buildExampleTests(cfg);

        System.out.println(tests);

        TestsRunner.runTests(cfg, tests);
    }
}
