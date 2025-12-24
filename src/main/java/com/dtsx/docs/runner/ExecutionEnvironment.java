package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.runner.drivers.ClientDriver;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.file.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

@RequiredArgsConstructor
public class ExecutionEnvironment implements AutoCloseable {
    private final VerifierConfig cfg;
    private final Path execEnvPath;

    @SneakyThrows
    public static ExecutionEnvironment setup(VerifierConfig cfg, ClientDriver driver) {
        val srcExecEnv = cfg.sourceExecutionEnvironment(driver.language());
        val destExecEnv = cfg.tmpFolder().resolve("environments").resolve(driver.language().name().toLowerCase());

        val execEnv = new ExecutionEnvironment(cfg, destExecEnv);
        execEnv.cleanIfNeeded();

        if (!Files.exists(destExecEnv)) {
            Files.createDirectories(destExecEnv);
            PathUtils.copyDirectory(srcExecEnv, destExecEnv);
        }

        return execEnv;
    }

    public <T> T useTestFile(Path sourceFile, Function<Path, T> function) {
        val testFile = setupFileForTesting(sourceFile, this);
        try {
            return function.apply(testFile);
        } finally {
            cleanupFileAfterTesting(testFile);
        }
    }

    public Path path() {
        return execEnvPath;
    }

    @Override
    public void close() {
        cleanIfNeeded();
    }

    @SneakyThrows
    private void cleanIfNeeded() {
        if (cfg.clean()) {
            PathUtils.deleteDirectory(execEnvPath);
        }
    }

    @SneakyThrows
    private Path setupFileForTesting(Path sourceFile, ExecutionEnvironment execEnv) {
        val dest = cfg.driver().resolveTestFileCopyDestination(execEnv);

        var content = Files.readString(sourceFile);
        content = SourceCodeReplacer.replacePlaceholders(content, cfg);
        content = cfg.driver().preprocessScript(cfg, content);

        Files.writeString(dest, content);
        return dest;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }
}
