package com.dtsx.docs.config;

import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.reporter.TestReporter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Accessors(fluent = true)
public class VerifierCtx {
    private final String token;
    private final String apiEndpoint;

    private final Path examplesFolder;
    private final Path snapshotsFolder;
    private final Path tmpFolder;

    @Getter(AccessLevel.NONE)
    private final Path environmentsFolder;

    private final ClientDriver driver;
    private final TestReporter reporter;
    private final String clientVersion;

    private final boolean clean;

    private final Map<ExternalProgramType, String[]> commandOverrides;

    public VerifierCtx(VerifierArgs internalArgs) {
        this.token = require(internalArgs.$token, "astra token", "-t", "ASTRA_TOKEN");
        this.apiEndpoint = require(internalArgs.$apiEndpoint, "API endpoint", "-e", "API_ENDPOINT");

        this.examplesFolder = requirePath(internalArgs.$examplesFolder, "examples folder", "-ef", "EXAMPLES_FOLDER");
        this.snapshotsFolder = requirePath(internalArgs.$snapshotsFolder, "snapshots folder", "-sf", "SNAPSHOTS_FOLDER");
        this.tmpFolder = requirePath(internalArgs.$tmpFolder, "temporary folder", "-tf", "TMP_FOLDER");
        this.environmentsFolder = Path.of("./resources/environments/");

        this.driver = ClientDriver.parse(require(internalArgs.$driver, "client driver", "-d", "DRIVER"));
        this.reporter = TestReporter.parse(internalArgs.$reporter);
        this.clientVersion = internalArgs.$clientVersion.orElse(driver.language().defaultVersion());

        this.clean = internalArgs.$clean;

        this.commandOverrides = mkCommandOverrides(internalArgs);

        verifyRequiredProgramsAvailable();
    }

    public Path sourceExecutionEnvironment(ClientLanguage lang) {
        return environmentsFolder.resolve(lang.name().toLowerCase());
    }

    private <T> T require(Optional<T> optional, String name, String flag, String envVar) {
        return optional.orElseThrow(() -> new IllegalArgumentException(
            "Missing required " + name + "; please provide it via the '" + flag + "' command line flag or the '" + envVar + "' environment variable."
        ));
    }

    private Path requirePath(String rawPath, String name, String flag, String envVar) {
        val path = Path.of(rawPath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                "The provided " + name + " path '" + path.toAbsolutePath() + "' does not exist; please provide a valid path via the '" + flag + "' command line flag or the '" + envVar + "' environment variable."
            );
        }
        return path;
    }

    private Map<ExternalProgramType, String[]> mkCommandOverrides(VerifierArgs internalArgs) {
        val overrides = new HashMap<ExternalProgramType, String[]>();

        for (val programType : ExternalProgramType.values()) {
            val envVarName = programType.name().toUpperCase() + "_COMMAND";

            Optional.ofNullable(System.getenv(envVarName)).or(() -> Optional.ofNullable(System.getProperty(envVarName))).ifPresent((value) -> {
                overrides.put(programType, value.split(" "));
            });
        }

        for (val override : internalArgs.$commandOverrides.entrySet()) {
            overrides.put(override.getKey(), override.getValue().split(" "));
        }

        return overrides;
    }

    private void verifyRequiredProgramsAvailable() {
        val requiredPrograms = new HashSet<Function<VerifierCtx, ExternalProgram>>() {{
            add(ExternalPrograms::tsx);
            add(ExternalPrograms::npm);
            addAll(driver.requiredPrograms());
        }};

        for (val mkProgram : requiredPrograms) {
            val program = mkProgram.apply(this);

            if (!program.exists()) {
                throw new IllegalStateException(program.name() + " could not be found. Please install it or set the " + program.envVar() + " environment variable.");
            }
        }
    }
}
