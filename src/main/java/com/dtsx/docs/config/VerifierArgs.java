package com.dtsx.docs.config;

import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import lombok.ToString;
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
        description = "Client driver to use (e.g., 'java', 'typescript').",
        defaultValue = "${CLIENT_DRIVER}",
        paramLabel = "DRIVER"
    )
    public Optional<String> $driver;

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
        defaultValue = "${TEST_REPORTER:-only_failures}",
        paramLabel = "TYPE"
    )
    public String $reporter;

    @Option(
        names = { "-cv", "--client-artifact" },
        description = "Client artifact to install (e.g., '@datastax/astra-db-ts@v2.0.0', '/path/to/local/package').",
        defaultValue = "${CLIENT_ARTIFACT}",
        paramLabel = "ARTIFACT"
    )
    public Optional<String> $clientVersion;

    @Option(
        names = { "--clean" },
        description = "Whether to clean the execution environment before running tests.",
        defaultValue = "${CLEAN:-false}"
    )
    public boolean $clean;

    @Option(
        names = { "-D", "--dry-run" },
        description = "If set, the verifier will perform a dry run without executing tests.",
        defaultValue = "${DRY_RUN:-false}"
    )
    public boolean $dryRun;

    @Option(
        names = { "-C", "--command-override" },
        description = "Override commands for external programs (e.g., `-Ctsx='npx tsx' -Cbash=/usr/bin/bash`).",
        paramLabel = "PROGRAM=COMMAND"
    )
    public Map<ExternalProgramType, String> $commandOverrides = Map.of();

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

    public VerifierCtx toCtx() {
        return new VerifierCtx(this);
    }
}
