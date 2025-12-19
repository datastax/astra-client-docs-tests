package com.dtsx.docs;

import com.dtsx.docs.drivers.ClientDriver;
import com.dtsx.docs.drivers.ClientLanguage;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class VerifierConfig {
    private final Dotenv dotenv;

    public final Commands commands = new Commands();

    public static VerifierConfig load() {
        return new VerifierConfig(Dotenv.configure().ignoreIfMissing().load());
    }

    public Path examplesFolder() {
        return requirePath(env("EXAMPLES_FOLDER", "./mock_examples/"));
    }

    public ClientDriver driver() {
        return ClientDriver.mkIfAvailable(env("DRIVER")).orElseThrow(() -> new IllegalStateException("No valid DRIVER specified in environment variables"));
    }

    public Path executionEnvironment(ClientLanguage lang) {
        return requirePath(Path.of(env("EXECUTION_ENVIRONMENTS", "./environments")).resolve(lang.name().toLowerCase()));
    }

    private String env(String key) {
        return dotenv.get(key, Optional.ofNullable(System.getenv(key)).orElseThrow(() -> new IllegalStateException("Environment variable " + key + " not found")));
    }

    private String env(String key, String defaultValue) {
        return dotenv.get(key, Optional.ofNullable(System.getenv(key)).orElse(defaultValue));
    }

    private Path requirePath(String str) {
        return requirePath(Path.of(str));
    }

    private Path requirePath(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Path " + path.toString() + " does not exist");
        }
        return path;
    }

    public class Commands {
        public String[] tsx() {
            return env("TSX_COMMAND", "npx tsx").split(" ");
        }
    }
}
