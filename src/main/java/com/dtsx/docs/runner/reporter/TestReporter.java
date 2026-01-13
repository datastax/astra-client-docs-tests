package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.TestPlanException;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults.TestOutcome.*;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

import static com.dtsx.docs.lib.ColorUtils.*;
import static com.dtsx.docs.runner.snapshots.verifier.VerifyMode.DRY_RUN;
import static java.util.stream.Collectors.joining;

/// The base class for test reporters that format/print test execution results.
///
/// There are two available reporters:
/// - `all_tests` ([AllTestsReporter]) - Shows all test results, passed and failed
/// - `only_failures` ([OnlyFailuresReporter]) - Only shows failed tests, hides passing ones
///
/// There are over 500 different documentation examples across multiple languages; the `only_failures` reporter
/// aides in reducing noise when most tests pass successfully.
///
/// @see AllTestsReporter
/// @see OnlyFailuresReporter
@RequiredArgsConstructor
public abstract class TestReporter {
    protected final VerifierCtx ctx;

    /// Prints the header at the start of test execution.
    ///
    /// Example:
    /// ```
    /// Running 123 tests...
    /// ```
    public void printHeader(TestPlan plan) {
        val prefix = (ctx.verifyMode() == DRY_RUN)
            ? "Dry-running"
            : "Running";

        CliLogger.println(false, "@|bold " + prefix + " @!" + plan.totalTests() + "!@ tests...|@");
    }

    /// Prints the heading for a base fixture group.
    ///
    /// Called once per base fixture before running its associated test.
    ///
    /// Example:
    /// ```
    /// 1) basic-collection.js
    /// ```
    public abstract void printBaseFixtureHeading(JSFixture baseFixture, TestResults history);

    /// Prints the results for a single test root.
    ///
    /// Called after each test root completes.
    ///
    /// Example:
    /// ```
    /// <header>
    ///   - ✓ dates (typescript,java)
    ///   - delete-many
    ///     - ✓ <examples_dir>/delete-many/example.ts
    ///     - ✗ <examples_dir>/delete-many/java/src/main/java/Example.java
    /// ```
    public abstract void printTestRootResults(JSFixture baseFixture, TestRootResults result, TestResults history);

    /// Prints the final summary after all tests complete (total, passed, failed, potentially bailed counts)
    ///
    /// Example:
    /// ```
    /// Test Summary:
    /// - Total tests: 123
    /// - Passed tests: 120
    /// - Failed tests: 3
    /// ```
    public void printSummary(TestPlan plan, TestResults history) {
        val skippedTests = plan.totalTests() - history.totalTests();

        CliLogger.println(true, "\n@|bold Test Summary:|@");
        CliLogger.println(true, "@!-!@ Total tests: " + plan.totalTests());
        CliLogger.println(true, "@!-!@ Passed tests: " + history.passedTests());
        CliLogger.println(true, "@!-!@ Failed tests: " + history.failedTests());

        if (skippedTests > 0) {
            CliLogger.println(true, "@!-!@ Bailed tests: " + skippedTests);
        }
        
        CliLogger.println(false);
        CliLogger.println(false, "@|bold View logs:|@");
        CliLogger.println(false, "@!$!@ open " + CliLogger.logFilePath(ctx));
    }

    /// Parses a reporter name and returns the corresponding reporter instance.
    /// - `all_tests` => [AllTestsReporter]
    /// - `only_failures` => [OnlyFailuresReporter]
    ///
    /// @param ctx the verifier context
    /// @param reporter the reporter name ("all_tests" or "only_failures")
    /// @return the reporter instance
    /// @throws TestPlanException if the reporter name is unknown
    public static TestReporter parse(VerifierCtx ctx, String reporter) {
        final Map<String, Function<VerifierCtx, TestReporter>> availableReporters = Map.of(
            "only_failures", OnlyFailuresReporter::new,
            "all_tests", AllTestsReporter::new
        );

        val supplier = availableReporters.get(reporter.toLowerCase());

        if (supplier == null) {
            throw new TestPlanException("Unknown reporter: '" + reporter + "' (expected one of: " + String.join(", ", availableReporters.keySet()) + ")");
        }

        return supplier.apply(ctx);
    }

    /// Helper to print a numbered fixture heading.
    ///
    /// Example:
    /// ```
    /// 1) basic-collection.js
    /// ```
    protected void printFixtureHeading(int index, JSFixture baseFixture) {
        CliLogger.println(false, "\n@|bold @!" + index + ") " + baseFixture.fixtureName() + "!@|@");
    }

    /// Helper to print test root results with status indicators.
    ///
    /// If all languages passed, it shows a compact single-line format:
    /// ```
    /// ✓ dates (typescript,python,java)
    /// ```
    ///
    /// If any failed, it shows a per-language breakdown:
    /// ```
    /// ✗ dates
    ///   ✓ ./path/to/example.ts
    ///   ✗ ./path/to/example.py
    ///   ! ./path/to/example.java
    /// ```
    protected void printTestRootResults(TestRootResults results) {
        val sb = new StringBuilder("  "); // this is not a weird face I promise

        val allResultsTheSame = results.outcomes().values().stream()
            .flatMap(m -> m.values().stream())
            .map(Object::getClass)
            .distinct()
            .count() == 1;

        CliLogger.println(
            false,
            (allResultsTheSame)
                ? mkShorthandReport(results, sb)
                : mkDetailedReport(results, sb)
        );
    }

    private String mkShorthandReport(TestRootResults results, StringBuilder sb) {
        val outcome = results.outcomes().values().stream().findFirst().flatMap(r -> r.values().stream().findFirst()).orElseThrow();

        val languages = results.outcomes().entrySet().stream()
            .map((e) -> {
                return Pair.of(
                    e.getKey().name().toLowerCase(), // language name
                    e.getValue().size() // file count for that language
                );
            })
            .sorted(Comparator.<Pair<String, Integer>>comparingInt(Pair::getRight).reversed())
            .map((p) -> {
                return (p.getRight() > 1)
                    ? p.getRight() + "×" + p.getLeft()
                    : p.getLeft();
            })
            .collect(joining(","));

        return sb
            .append(outputPrefix(outcome))
            .append(" ")
            .append(results.testRoot().rootName())
            .append(Style.faint.on())
            .append(" (")
            .append(languages)
            .append(")")
            .append(Style.faint.off())
            .toString();
    }

    private String mkDetailedReport(TestRootResults results, StringBuilder sb) {
        val shorthandSymbol = results.allPassed()
            ? "@|green ✓|@"
            : "@|red ✗|@";

        sb
            .append(shorthandSymbol)
            .append(" ")
            .append(results.testRoot().rootName());

        results.outcomes().forEach((_, outcomes) -> {
            outcomes.forEach((path, outcome) -> {
                sb.append(highlight("\n    "))
                    .append(outputPrefix(outcome))
                    .append(" ")
                    .append(color(Style.faint, results.testRoot().displayPath(path)));
            });
        });

        return sb.toString();
    }

    private String outputPrefix(TestOutcome outcome) {
        return switch (outcome) {
            case Passed _ -> "@|green ✓|@";
            case DryPassed _ -> "@!?!@";
            case FailedToVerify _, FailedToCompile _ -> "@|red ✗|@";
            case Mismatch _ -> "@|red M|@";
            case Errored _ -> "@|yellow !|@";
        };
    }
}
