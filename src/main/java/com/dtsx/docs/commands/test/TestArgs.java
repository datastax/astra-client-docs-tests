package com.dtsx.docs.commands.test;

import com.dtsx.docs.config.args.BaseScriptRunnerArgs;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.VerifyMode;
import com.dtsx.docs.core.runner.tests.reporter.TestReporter;
import com.dtsx.docs.core.runner.tests.reporter.TestReporters;
import lombok.ToString;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Optional;

@ToString
public class TestArgs extends BaseScriptRunnerArgs<TestCtx> {
    @Parameters(
        description = "Client drivers to use (e.g., 'java', 'typescript').",
        defaultValue = "${CLIENT_DRIVERS}",
        completionCandidates = ClientDriver.Completions.class,
        paramLabel = "DRIVER",
        split = ","
    )
    public List<String> $drivers;

    @Option(
        names = { "-r", "--test-reporter" },
        description = "Test reporter type (e.g., 'only_failures', 'all_tests').",
        defaultValue = "${TEST_REPORTER:-all_tests}",
        paramLabel = "TYPE"
    )
    public TestReporters $reporter;

    @Option(
        names = { "-m", "--verify-mode" },
        description = "Verification mode to use (normal, verify_only, compile_only, dry_run).",
        defaultValue = "${VERIFY_MODE:-normal}",
        paramLabel = "MODE"
    )
    public VerifyMode $verifyMode;

    @Option(
        names = { "-D", "--dry-run" },
        description = "Short for `--verify-mode dry_run`."
    )
    public boolean $dryRun;

    @Option(
        names = { "-f", "--filter" },
        description = "Comma-separated regex filters to select specific tests to run. Can be used multiple times.",
        defaultValue = "${FILTERS:-}",
        paramLabel = "FILTER",
        split = ","
    )
    public List<String> $filters;

    @Option(
        names = { "-F", "--filter-not" },
        description = "Comma-separated regex filters to select specific tests to exclude. Can be used multiple times.",
        defaultValue = "${INVERSE_FILTERS:-}",
        paramLabel = "FILTER",
        split = ","
    )
    public List<String> $inverseFilters;

    @Override
    public TestCtx toCtx(CommandSpec spec) {
        return new TestCtx(this, spec);
    }
}
