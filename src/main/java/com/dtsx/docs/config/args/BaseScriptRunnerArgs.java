package com.dtsx.docs.config.args;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.Optional;

public abstract class BaseScriptRunnerArgs<Ctx extends BaseScriptRunnerCtx> extends BaseArgs<Ctx> {
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
        names = { "--clean" },
        description = "Whether to clean the execution environment before running tests.",
        defaultValue = "${CLEAN:-false}"
    )
    public boolean $clean;

    @Option(
        names = { "-C", "--command-override" },
        description = "Override commands for external programs (e.g., `-Ctsx='npx -y tsx' -Cbash=/usr/bin/bash`).",
        paramLabel = "PROGRAM=COMMAND"
    )
    public Map<ExternalProgramType, String> $commandOverrides = Map.of();

    @Option(
        names = { "-b", "--bail" },
        description = "Whether to stop execution upon the first failure.",
        defaultValue = "${BAIL:-false}"
    )
    public boolean $bail;

    @Option(
        names = { "-ef", "--examples-folder" },
        description = "Path to the folder containing example projects.",
        defaultValue = "${EXAMPLES_FOLDER:-resources/mock_examples}",
        paramLabel = "FOLDER"
    )
    public String $examplesFolder;
}
