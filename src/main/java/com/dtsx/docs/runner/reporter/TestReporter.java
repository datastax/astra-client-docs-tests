package com.dtsx.docs.runner.reporter;

import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.TestPlanException;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import lombok.RequiredArgsConstructor;
import lombok.val;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.Map;
import java.util.function.Function;

import static com.dtsx.docs.lib.ColorUtils.color;
import static com.dtsx.docs.lib.ColorUtils.highlight;
import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor
public abstract class TestReporter {
    protected final VerifierCtx ctx;

    public void printHeader(TestPlan plan) {
        CliLogger.println("@|bold Running @!" + plan.totalTests() + "!@ tests...|@");
    }

    public abstract void printBaseFixtureHeading(JSFixture baseFixture, TestResults history);

    public abstract void printTestRootResults(JSFixture baseFixture, TestRootResults result, TestResults history);

    public void printSummary(TestResults history) {
        CliLogger.println("\n@|bold Test Summary:|@");
        CliLogger.println("@!-!@ Total tests: " + history.totalTests());
        CliLogger.println("@!-!@ Passed tests: " + history.passedTests());
        CliLogger.println("@!-!@ Failed tests: " + history.failedTests());
    }

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

    protected void printFixtureHeading(int index, JSFixture baseFixture) {
        CliLogger.println("\n@|bold @!" + index + ") " + baseFixture.fixtureName() + "!@|@");
    }

    protected void printTestRootResults(TestRootResults results) {
        val sb = new StringBuilder(highlight("  - " ));

        if (results.allApproved()) {
            val resultSample = results.outcomes().values().stream().findFirst().orElseThrow();

            sb
                .append(resultSample.status())
                .append(" ")
                .append(results.testRoot().rootName(ctx))
                .append(Style.faint.on())
                .append(" (")
                .append(results.testRoot().filesToTest().keySet().stream().map(l -> l.name().toLowerCase()).collect(joining(",")))
                .append(")")
                .append(Style.faint.off());
        } else {
            sb.append(results.testRoot().rootName(ctx));

            for (val entry : results.outcomes().entrySet()) {
                val language = entry.getKey();
                val outcome = entry.getValue();

                sb.append(highlight("\n    - "))
                    .append(outcome.status())
                    .append(" ")
                    .append(color(Style.faint, results.testRoot().filesToTest().get(language).toString()));
            }
        }

        CliLogger.println(sb.toString());
    }
}
