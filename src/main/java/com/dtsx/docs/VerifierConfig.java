package com.dtsx.docs;

import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.reporter.TestReporter;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class VerifierConfig {
    private final Dotenv dotenv;

    public final Commands commands = new Commands();

    public static VerifierConfig load() {
        val cfg = new VerifierConfig(Dotenv.configure().load());
        verifyRequiredProgramsAvailable(cfg);
        return cfg;
    }

    private static void verifyRequiredProgramsAvailable(VerifierConfig cfg) {
        val requiredPrograms = new HashSet<Function<VerifierConfig, ExternalProgram>>() {{
            add(ExternalPrograms::tsx);
            addAll(cfg.driver().requiredPrograms());
        }};

        for (val mkProgram : requiredPrograms) {
            val program = mkProgram.apply(cfg);

            if (!program.exists()) {
                throw new IllegalStateException(program.name() + " could not be found. Please install it or set the " + program.envVar() + " environment variable.");
            }
        }
    }

    public String token() {
        return env("APPLICATION_TOKEN");
    }

    public String apiEndpoint() {
        return env("API_ENDPOINT");
    }

    public Path examplesFolder() {
        return requirePath(env("EXAMPLES_FOLDER", "./mock_examples/"));
    }

    public Path tmpFolder() {
        return Path.of(env("TMP_FOLDER", "./tmp/"));
    }

    private @Nullable ClientDriver driver = null;

    public ClientDriver driver() {
        if (driver == null) {
            driver = ClientDriver.parse(env("DRIVER"));
        }
        return driver;
    }

    private @Nullable TestReporter reporter = null;

    public TestReporter reporter() {
        if (reporter == null) {
            reporter = TestReporter.parse(env("REPORTER", "only_failures"));
        }
        return reporter;
    }

    public Path sourceExecutionEnvironment(ClientLanguage lang) {
        return requirePath(Path.of(env("EXECUTION_ENVIRONMENTS_FOLDER", "./environments")).resolve(lang.name().toLowerCase()));
    }

    public File snapshotsFolder() {
        return requirePath(Path.of(env("SNAPSHOTS_FOLDER", "./snapshots"))).toFile();
    }

    public boolean clean() {
        return Boolean.parseBoolean(env("CLEAN_EXECUTION_ENVIRONMENT", "false"));
    }

    public class Commands {
        public String[] tsx() {
            return env("TSX_COMMAND", "npx tsx").split(" ");
        }

        public String[] npm() {
            return env("NPM_COMMAND", "npm").split(" ");
        }
    }

    private String env(String key) {
        if (dotenv.get(key) == null) {
            return Optional.ofNullable(System.getenv(key)).orElseThrow(() -> new IllegalStateException("Environment variable " + key + " not found"));
        }
        return dotenv.get(key);
    }

    private String env(String key, String defaultValue) {
        return dotenv.get(key, Optional.ofNullable(System.getenv(key)).orElse(defaultValue));
    }

    private Path requirePath(String str) {
        return requirePath(Path.of(str));
    }

    private Path requirePath(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Path " + path + " does not exist");
        }
        return path;
    }
}
