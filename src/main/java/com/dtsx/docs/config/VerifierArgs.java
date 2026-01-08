package com.dtsx.docs.config;

import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.verifier.VerifyMode;
import lombok.ToString;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ToString
public class VerifierArgs {
    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Client drivers to use (e.g., 'java', 'typescript').",
        defaultValue = "${CLIENT_DRIVERS}",
        paramLabel = "DRIVER",
        split = ","
    )
    public List<ClientLanguage> $drivers;

    @Option(
        names = { "-A", "--client-artifact" },
        description = "Client artifacts to install (e.g.,`-Atypescript=@datastax/astra-db-ts@v2.0.0' -Apython=/path/to/local/package`).",
        paramLabel = "CLIENT=ARTIFACT"
    )
    public Map<ClientLanguage, String> $artifactOverrides = Map.of();

    @Option(
        names = { "-t", "--astra-token" },
        description = "Astra token",
        defaultValue = "${ASTRA_TOKEN}",
        paramLabel = "TOKEN"
    )
    public Optional<String> $token;

    @Option(
        names = { "-e", "--api-endpoint" },
        description = "Test database API endpoint.",
        defaultValue = "${API_ENDPOINT}",
        paramLabel = "ENDPOINT"
    )
    public Optional<String> $apiEndpoint;

    @Option(
        names = { "-ef", "--examples-folder" },
        description = "Path to the folder containing example projects.",
        defaultValue = "${EXAMPLES_FOLDER:-./resources/mock_examples}",
        paramLabel = "FOLDER"
    )
    public String $examplesFolder;

    @Option(
        names = { "-sf", "--snapshots-folder" },
        description = "Path to the folder containing snapshots.",
        defaultValue = "${SNAPSHOTS_FOLDER:-./snapshots}",
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
        names = { "--clean" },
        description = "Whether to clean the execution environment before running tests.",
        defaultValue = "${CLEAN:-false}"
    )
    public boolean $clean;

    @Option(
        names = { "-C", "--command-override" },
        description = "Override commands for external programs (e.g., `-Ctsx='npx tsx' -Cbash=/usr/bin/bash`).",
        paramLabel = "PROGRAM=COMMAND"
    )
    public Map<ExternalProgramType, String> $commandOverrides = Map.of();

    @Option(
        names = { "-m", "--verify-mode" },
        description = "Verification mode to use (normal, verify_only, dry_run).",
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
        names = { "--spinner" },
        description = "enable/disable spinner in the CLI output.",
        defaultValue = "${SPINNER:-true}",
        fallbackValue = "true",
        negatable = true
    )
    public void $spinner(boolean enabled) {
        CliLogger.setSpinnerEnabled(enabled);
    }

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

    public VerifierCtx toCtx(CommandSpec spec) {
        return new VerifierCtx(this, spec);
    }
}
