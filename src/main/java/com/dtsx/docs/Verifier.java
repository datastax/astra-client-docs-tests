package com.dtsx.docs;

import com.dtsx.docs.builder.TestsBuilder;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.TestRunner;
import lombok.val;

import java.util.HashSet;
import java.util.function.Function;

public class Verifier {
    public static void main(String[] args) {
        val cfg = VerifierConfig.load();

        verifyRequiredProgramsAvailable(cfg);

        val tests = TestsBuilder.buildExampleTests(cfg);

        val results = TestRunner.runTests(cfg, tests);

        val output = cfg.reporter().compileResults(results);

        System.out.println(output);
    }

    private static void verifyRequiredProgramsAvailable(VerifierConfig cfg) {
        val requiredPrograms = new HashSet<Function<VerifierConfig, ExternalProgram>>() {{
            add(ExternalPrograms::tsx);
            addAll(cfg.driver().requiredPrograms());
        }};

        for (val mkProgram : requiredPrograms) {
            val program = mkProgram.apply(cfg);

            if (!program.exists()) {
                throw new IllegalStateException(program.name() + " could not be found. Please install it or set the " + program.envVar() + " environment variable.");
            }
        }
    }
}
