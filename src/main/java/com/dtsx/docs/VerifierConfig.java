package com.dtsx.docs;

import com.dtsx.docs.reporter.TestReporter;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class VerifierConfig {
    private final Dotenv dotenv;

    public final Commands commands = new Commands();

    public static VerifierConfig load() {
        return new VerifierConfig(Dotenv.configure().load());
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

    public ClientDriver driver() {
        return ClientDriver.parse(env("DRIVER"));
    }

    public TestReporter reporter() {
        return TestReporter.parse(env("REPORTER", "only_failures"));
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
