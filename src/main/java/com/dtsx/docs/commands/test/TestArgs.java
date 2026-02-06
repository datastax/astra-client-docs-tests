package com.dtsx.docs.commands.test;

import com.dtsx.docs.config.args.BaseScriptRunnerArgs;
import com.dtsx.docs.core.runner.tests.VerifyMode;
import lombok.ToString;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@ToString
public class TestArgs extends BaseScriptRunnerArgs<TestCtx> {
    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Client drivers to use (e.g., 'java', 'typescript').",
        defaultValue = "${CLIENT_DRIVERS}",
        paramLabel = "DRIVER",
        split = ","
    )
    public List<String> $drivers;

    @Option(
        names = { "-sf", "--snapshots-folder" },
        description = "Path to the folder containing snapshots.",
        defaultValue = "${SNAPSHOTS_FOLDER:-snapshots}",
        paramLabel = "FOLDER"
    )
    public String $snapshotsFolder;

    @Option(
        names = { "-r", "--test-reporter" },
        description = "Test reporter type (e.g., 'only_failures', 'all_tests').",
        defaultValue = "${TEST_REPORTER:-all_tests}",
        paramLabel = "TYPE"
    )
    public String $reporter;

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
