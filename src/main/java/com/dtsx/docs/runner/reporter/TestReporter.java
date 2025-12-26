package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestResult;
import lombok.val;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public abstract class TestReporter {
    public void printHeader(TestPlan plan) {
        CliLogger.println("Running " + countTotalTests(plan) + " tests...");
    }

    public abstract void printBaseFixtureHeading(JSFixture baseFixture, TestResults history);

    public abstract void printTestResult(JSFixture baseFixture, TestResult result, TestResults history);

    public void printSummary(TestResults history) {
        val totalTests = countTotalTests(history);
        val passedTests = countPassedTests(history);
        val failedTests = totalTests - passedTests;

        CliLogger.println("\nTest Summary:");
        CliLogger.println("- Total tests: " + totalTests);
        CliLogger.println("- Passed tests: " + passedTests);
        CliLogger.println("- Failed tests: " + failedTests);
    }

    public static TestReporter parse(String reporter) {
        final Map<String, Supplier<TestReporter>> availableReporters = Map.of(
            "only_failures", OnlyFailuresReporter::new,
            "all_tests", AllTestsReporter::new
        );

        val supplier = availableReporters.get(reporter.toLowerCase());

        if (supplier == null) {
            throw new IllegalArgumentException("Unknown reporter: '" + reporter + "' (expected one of: " + String.join(", ", availableReporters.keySet()) + ")");
        }

        return supplier.get();
    }

    protected void printFixtureHeading(int index, JSFixture baseFixture) {
        CliLogger.println("\n" + index + ") " + baseFixture.fixtureName());
    }

    private int countTotalTests(TestPlan plan) {
        return plan.unwrap().values().stream().mapToInt(Set::size).sum();
    }

    private int countTotalTests(TestResults history) {
        return history.unwrap().values().stream().mapToInt(List::size).sum();
    }

    private int countPassedTests(TestResults history) {
        return history.unwrap().values().stream().flatMap(List::stream).mapToInt(result -> result.approved() ? 1 : 0).sum();
    }
}
