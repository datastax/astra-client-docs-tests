package com.dtsx.docs.config;

import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.reporter.TestReporter;
import com.dtsx.docs.runner.verifier.VerifyMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public class VerifierCtx {
    private final ConnectionInfo connectionInfo;

    private final Path examplesFolder;
    private final Path snapshotsFolder;
    private final Path tmpFolder;

    @Getter(AccessLevel.NONE)
    private final Path execEnvTemplatesFolder;

    private final Map<ClientLanguage, ClientDriver> drivers;
    private final TestReporter reporter;
    private final VerifyMode verifyMode;

    private final boolean clean;

    private final Map<ExternalProgramType, String[]> commandOverrides;
    private final Predicate<Path> filter;

    public VerifierCtx(VerifierArgs args, CommandSpec spec) {
        val cmd = spec.commandLine();

        this.connectionInfo = mkConnectionInfo(cmd, args);

        this.examplesFolder = ArgUtils.requirePath(cmd, args.$examplesFolder, "examples folder", "-ef", "EXAMPLES_FOLDER");
        this.snapshotsFolder = ArgUtils.requirePath(cmd, args.$snapshotsFolder, "snapshots folder", "-sf", "SNAPSHOTS_FOLDER");
        this.tmpFolder = Path.of("./.docs_tests_temp");
        this.execEnvTemplatesFolder = Path.of("./resources/environments/");

        this.drivers = mkDrivers(cmd, args);
        this.reporter = TestReporter.parse(this, args.$reporter);
        this.verifyMode = resolveVerifyMode(args);

        this.clean = args.$clean;

        this.commandOverrides = mkCommandOverrides(args);
        this.filter = mkFilter(args.$filters, args.$inverseFilters);

        verifyRequiredProgramsAvailable(cmd);
    }

    private ConnectionInfo mkConnectionInfo(CommandLine cmd, VerifierArgs args) {
        val token = ArgUtils.requireFlag(cmd, args.$token, "astra token", "-t", "ASTRA_TOKEN");
        val apiEndpoint = ArgUtils.requireFlag(cmd, args.$apiEndpoint, "API endpoint", "-e", "API_ENDPOINT");
        return ConnectionInfo.parse(token, apiEndpoint);
    }

    public Path executionEnvironmentTemplate(ClientLanguage lang) {
        return execEnvTemplatesFolder.resolve(lang.name().toLowerCase());
    }

    public List<ClientLanguage> languages() {
        return new ArrayList<>(drivers.keySet());
    }

    private Map<ClientLanguage, ClientDriver> mkDrivers(CommandLine cmd, VerifierArgs args) {
        ArgUtils.requireParameter(cmd, args.$drivers.stream().findFirst(), "client driver", 1, "CLIENT_DRIVER");

        val driversMap = new HashMap<ClientLanguage, ClientDriver>();

        for (val lang : args.$drivers) {
            val usesArtifact = lang.defaultArtifact() != null;

            val envVarName = lang.name().toUpperCase() + "_ARTIFACT";
            val resolvedArtifact = Optional.ofNullable(args.$artifactOverrides.get(lang)).or(() -> Optional.ofNullable(System.getProperty(envVarName)));

            if (resolvedArtifact.isPresent() && !usesArtifact) {
                throw new ParameterException(cmd, lang.name() + " does not support artifact overrides.");
            }

            driversMap.put(lang, lang.mkDriver().apply(resolvedArtifact.orElse(lang.defaultArtifact())));
        }

        return driversMap;
    }

    private VerifyMode resolveVerifyMode(VerifierArgs args) {
        return (args.$dryRun)
            ? VerifyMode.DRY_RUN
            : args.$verifyMode;
    }

    private Map<ExternalProgramType, String[]> mkCommandOverrides(VerifierArgs args) {
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

    private Predicate<Path> mkFilter(List<String> filters, List<String> inverseFilters) {
        val includePredicate = mkFilterPredicates(filters)
            .orElse(_ -> true);

        val excludePredicate = mkFilterPredicates(inverseFilters)
            .map(Predicate::negate)
            .orElse(_ -> true);

        return includePredicate.and(excludePredicate);
    }

    private Optional<Predicate<Path>> mkFilterPredicates(List<String> filter) {
        return filter.stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map((s) -> (Predicate<Path>) (path) -> path.toString().matches(".*" + s + ".*"))
            .reduce(Predicate::or);
    }

    private void verifyRequiredProgramsAvailable(CommandLine cmd) {
        val requiredPrograms = new HashSet<Function<VerifierCtx, ExternalProgram>>() {{
            add(ExternalPrograms::tsx);
            add(ExternalPrograms::npm);

            for (val driver : drivers.values()) {
                addAll(driver.requiredPrograms());
            }
        }};

        for (val mkProgram : requiredPrograms) {
            val program = mkProgram.apply(this);

            if (!program.exists()) {
                throw new ParameterException(cmd, program.name() + " could not be found. Please install it or set the " + program.envVar() + " environment variable.");
            }
        }
    }
}
