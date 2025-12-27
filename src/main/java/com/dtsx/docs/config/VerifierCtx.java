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
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private final Predicate<Path> filter;

    public VerifierCtx(VerifierArgs internalArgs) {
        this.token = requireFlag(internalArgs.$token, "astra token", "-t", "ASTRA_TOKEN");
        this.apiEndpoint = requireFlag(internalArgs.$apiEndpoint, "API endpoint", "-e", "API_ENDPOINT");

        this.examplesFolder = requirePath(internalArgs.$examplesFolder, "examples folder", "-ef", "EXAMPLES_FOLDER");
        this.snapshotsFolder = requirePath(internalArgs.$snapshotsFolder, "snapshots folder", "-sf", "SNAPSHOTS_FOLDER");
        this.tmpFolder = Path.of("./.docs_tests_temp");
        this.environmentsFolder = Path.of("./resources/environments/");

        this.driver = ClientDriver.parse(requireParameter(internalArgs.$driver, "client driver", 1, "CLIENT_DRIVER"));
        this.reporter = TestReporter.parse(internalArgs.$reporter);
        this.clientVersion = internalArgs.$clientVersion.orElse(driver.language().defaultVersion());

        this.clean = internalArgs.$clean;

        this.commandOverrides = mkCommandOverrides(internalArgs);
        this.filter = mkFilter(internalArgs.$filters, internalArgs.$inverseFilters);

        verifyRequiredProgramsAvailable();
    }

    public Path sourceExecutionEnvironment(ClientLanguage lang) {
        return environmentsFolder.resolve(lang.name().toLowerCase());
    }

    private <T> T requireFlag(Optional<T> optional, String name, String flag, String envVar) {
        return optional.orElseThrow(() -> new IllegalArgumentException(
            "Missing required option " + name + "; please provide it via the '" + flag + "' command line flag or the '" + envVar + "' environment variable."
        ));
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T requireParameter(Optional<T> parameter, String name, int index, String envVar) {
        return parameter.orElseThrow(() -> new IllegalArgumentException(
            "Missing required parameter " + name + "; please provide it as parameter #" + index + " or via the '" + envVar + "' environment variable."
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
