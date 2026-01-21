package com.dtsx.docs.core.runner.tests.results;

import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dtsx.docs.lib.CliLogger.captureStackTrace;

public sealed interface TestOutcome {
    default String name() {
        return this.getClass().getSimpleName();
    }

    default boolean passed() {
        return this instanceof Passed || this instanceof DryPassed;
    }

    enum Passed implements TestOutcome {
        INSTANCE
    }

    enum DryPassed implements TestOutcome {
        INSTANCE
    }

    enum Mismatch implements TestOutcome {
        INSTANCE;

        public Mismatch alsoLog(TestRoot testRoot, ClientLanguage language, Map<String, Set<Path>> differingSnapshots) {
            val extra = new StringBuilder("Differing snapshots found for the following files:\n");

            for (val entry : differingSnapshots.entrySet()) {
                extra.append("Snapshot:\n").append(entry.getKey()).append("\nFiles:\n");

                for (val path : entry.getValue()) {
                    extra.append(" - ").append(path).append("\n");
                }
            }

            logResult(testRoot, language, this, extra.toString());
            return this;
        }
    }

    record FailedToVerify(Optional<Path> expected) implements TestOutcome {
        public FailedToVerify alsoLog(TestRoot testRoot, ClientLanguage language, String actualSnapshot) {
            val prefix = expected
                .map((path) -> "Expected snapshot file: " + path)
                .orElse("No approved snapshot file found.");

            val extra = prefix + "\nActual snapshot:\n" + actualSnapshot;

            logResult(testRoot, language, this, extra);
            return this;
        }
    }

    record FailedToCompile(String message) implements TestOutcome {
        public FailedToCompile alsoLog(TestRoot testRoot, ClientLanguage language, String output) {
            logResult(testRoot, language, this, "Compilation output:\n" + output);
            return this;
        }
    }

    record Errored(Exception error) implements TestOutcome {  // TODO consider actually using this somewhere
        public Errored alsoLog(TestRoot testRoot, ClientLanguage language) {
            logResult(testRoot, language, this, captureStackTrace(error));
            return this;
        }
    }

    private static void logResult(TestRoot testRoot, ClientLanguage language, TestOutcome testOutcome, String extra) {
        CliLogger.failed(testRoot.rootName() + " (" + language + ") => " + testOutcome.name());

        if (!extra.isBlank()) {
            CliLogger.failed(extra);
        }
    }
}
