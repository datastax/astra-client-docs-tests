package com.dtsx.docs.config;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import lombok.ToString;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.Optional;

@ToString
public class VerifierArgs {
    @Option(
        names = { "-t", "--astra-token" },
        description = "Astra token",
        defaultValue = "${ASTRA_TOKEN}"
    )
    public Optional<String> $token;

    @Option(
        names = { "-e", "--api-endpoint" },
        description = "Test database API endpoint.",
        defaultValue = "${API_ENDPOINT}"
    )
    public Optional<String> $apiEndpoint;

    @Option(
        names = { "-ef", "--examples-folder" },
        description = "Path to the folder containing example projects.",
        defaultValue = "${EXAMPLES_FOLDER:-./resources/mock_examples}"
    )
    public String $examplesFolder;

    @Option(
        names = { "-sf", "--snapshots-folder" },
        description = "Path to the folder containing snapshots.",
        defaultValue = "${SNAPSHOTS_FOLDER:-./resources/snapshots}"
    )
    public String $snapshotsFolder;

    @Option(
        names = { "-tf", "--tmp-folder" },
        description = "Path to the temporary folder.",
        defaultValue = "${TMP_FOLDER:-./tmp}"
    )
    public String $tmpFolder;

    @Option(
        names = { "-c", "--client-driver" },
        description = "Client driver to use (e.g., 'java', 'typescript').",
        defaultValue = "${CLIENT_DRIVER}"
    )
    public Optional<String> $driver;

    @Option(
        names = { "-r", "--test-reporter" },
        description = "Test reporter type (e.g., 'only_failures', 'all_tests').",
        defaultValue = "${TEST_REPORTER:-only_failures}"
    )
    public String $reporter;

    @Option(
        names = { "-cv", "--client-version" },
        description = "Client version to install (e.g., '@datastax/astra-db-ts@v2.0.0', '/path/to/local/package').",
        defaultValue = "${CLIENT_VERSION}"
    )
    public Optional<String> $clientVersion;

    @Option(
        names = { "--clean" },
        description = "Whether to clean the execution environment before running tests.",
        defaultValue = "${CLEAN:-false}"
    )
    public boolean $clean;

    @Option(
        names = { "-C", "--command-override" },
        description = "Override commands for external programs (e.g., `-Ctsx='npx tsx' -Cbash=/usr/bin/bash`)."
    )
    public Map<ExternalProgramType, String> $commandOverrides;

    public VerifierCtx toCtx() {
        return new VerifierCtx(this);
    }
}
