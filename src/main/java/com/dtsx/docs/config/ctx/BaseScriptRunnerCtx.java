package com.dtsx.docs.config.ctx;

import com.dtsx.docs.config.ArgUtils;
import com.dtsx.docs.config.ConnectionInfo;
import com.dtsx.docs.config.args.BaseScriptRunnerArgs;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
public abstract class BaseScriptRunnerCtx extends BaseCtx {
    /// Resolved from `EXAMPLES_FOLDER` env var, with fallback to `./resources/mock_examples`.
    ///
    /// If the provided path doesn't have `_base/` and `_fixtures/` subdirectories,
    /// automatically tries `<path>/modules/api-reference/examples` (for docs repo structure).
    private final Path examplesFolder;

    /// Parsed from `-t`/`--astra-token` flag or `ASTRA_TOKEN` env var, and `-e`/`--api-endpoint` flag or `API_ENDPOINT` env var.
    ///
    /// The token must have read/write access to the test database, and the endpoint
    /// determines whether we're testing against a collection or table API.
    private final ConnectionInfo connectionInfo;

    @Getter(AccessLevel.NONE)
    private final Path execEnvTemplatesFolder;

    /// Set by `--clean` flag or `CLEAN` env var (default: false).
    ///
    /// When true, deletes `.docs_tests_temp/` after all tests complete.
    private final boolean clean;

    /// Built from `<PROGRAM>_COMMAND` env vars (e.g., `TSX_COMMAND=npx -y tsx`).
    ///
    /// Allows overriding default program paths when they're not in PATH or you need
    /// a specific version (e.g., `PYTHON_COMMAND=python3.11`).
    private final Map<ExternalProgramType, String[]> commandOverrides;

    /// Set by `--bail` flag or `BAIL` env var (default: false).
    ///
    /// When true, stops execution upon the first failure.
    private final boolean bail;

    /// Returns `resources/environments/<language>/` containing the base project structure.
    ///
    /// For example, `typescript/` contains `package.json`, `java/` contains `build.gradle`.
    /// These are copied to `.docs_tests_temp/execution_environments/<language>/` at runtime.
    public Path executionEnvironmentTemplate(ClientLanguage lang) {
        return execEnvTemplatesFolder.resolve(lang.name().toLowerCase());
    }

    public BaseScriptRunnerCtx(BaseScriptRunnerArgs<?> args, CommandSpec spec) {
        super(spec);
        this.examplesFolder = resolveExampleFolder(spec.commandLine(), args).toAbsolutePath().normalize();
        this.connectionInfo = mkConnectionInfo(cmd, args);
        this.execEnvTemplatesFolder = Path.of("resources/environments/");
        this.clean = args.$clean;
        this.commandOverrides = mkCommandOverrides(args);
        this.bail = args.$bail;
    }

    private ConnectionInfo mkConnectionInfo(CommandLine cmd, BaseScriptRunnerArgs<?> args) {
        val token = ArgUtils.requireFlag(cmd, args.$token, "astra token", "-t", "ASTRA_TOKEN");
        val apiEndpoint = ArgUtils.requireFlag(cmd, args.$apiEndpoint, "API endpoint", "-e", "API_ENDPOINT");
        return new ConnectionInfo(token, apiEndpoint);
    }

    private Map<ExternalProgramType, String[]> mkCommandOverrides(BaseScriptRunnerArgs<?> args) {
        val overrides = new HashMap<ExternalProgramType, String[]>();

        for (val programType : ExternalProgramType.values()) {
            val envVarName = programType.name().toUpperCase() + "_COMMAND";

            Optional.ofNullable(System.getenv(envVarName)).or(() -> Optional.ofNullable(System.getProperty(envVarName))).ifPresent((value) -> {
                overrides.put(programType, value.split(" "));
            });
        }

        for (val override : args.$commandOverrides.entrySet()) {
            overrides.put(override.getKey(), override.getValue().split(" "));
        }

        return overrides;
    }

    private Path resolveExampleFolder(CommandLine cmd, BaseScriptRunnerArgs<?> args) {
        val folder = ArgUtils.requirePath(cmd, args.$examplesFolder, "examples folder", "-ef", "EXAMPLES_FOLDER");

        Predicate<Path> isValidExampleFolder = (path) -> {
            return Stream.of(path, path.resolve("_base"), path.resolve("_fixtures")).allMatch(Files::isDirectory);
        };

        if (isValidExampleFolder.test(folder)) {
            return folder;
        }

        val nestedFolder = folder.resolve("modules/api-reference/examples");

        if (isValidExampleFolder.test(nestedFolder)) {
            return nestedFolder;
        }

        throw new ParameterException(cmd, "Neither " + folder + " nor " + nestedFolder + " is the expected examples folder.");
    }

    protected void verifyRequiredProgramsAvailable(HashSet<Function<BaseScriptRunnerCtx, ExternalProgram>> programs, CommandLine cmd) {
        programs.add(ExternalPrograms::bash);
        programs.add(ExternalPrograms::jq);
        
        for (val mkProgram : programs) {
            val program = mkProgram.apply(this);

            if (!program.exists()) {
                throw new ParameterException(cmd, program.name() + " could not be found. Please install it or set the " + program.envVar() + " environment variable.");
            }
        }
    }

    protected ClientDriver mkDriverForLanguage(CommandLine cmd, ClientLanguage lang, BaseScriptRunnerArgs<?> args) {
        val usesArtifact = lang.defaultArtifact() != null;

        val envVarName = lang.name().toUpperCase() + "_ARTIFACT";

        val resolvedArtifact = Optional.ofNullable(args.$artifactOverrides.get(lang))
            .or(() -> Optional.ofNullable(System.getProperty(envVarName)));

        if (resolvedArtifact.isPresent() && !usesArtifact) {
            throw new ParameterException(cmd, lang.name() + " does not support artifact overrides.");
        }

        return lang.mkDriver().apply(resolvedArtifact.orElse(lang.defaultArtifact()));
    }
}
