package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.drivers.ClientDriver;
import lombok.*;
import org.apache.commons.io.file.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ExecutionEnvironment implements AutoCloseable {
    private final VerifierCtx ctx;
    private final Path execEnvPath;

    @With(AccessLevel.PRIVATE)
    private final Path testFileCopyPath;

    public static ExecutionEnvironment setup(VerifierCtx ctx, ClientDriver driver) {
        val languageName = driver.language().name().toLowerCase();

        return CliLogger.loading("Setting up %s execution environment".formatted(languageName), (_) -> {
            val srcExecEnv = ctx.sourceExecutionEnvironment(driver.language());
            val destExecEnv = ctx.tmpFolder().resolve("environments").resolve(driver.language().name().toLowerCase());

            val execEnv = new ExecutionEnvironment(ctx, destExecEnv, null);
            execEnv.cleanIfNeeded();

            try {
                if (!Files.exists(destExecEnv)) {
                    Files.createDirectories(destExecEnv);
                    PathUtils.copyDirectory(srcExecEnv, destExecEnv);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup " + languageName + " execution environment", e);
            }

            val testFileCopyPath = driver.setupExecutionEnvironment(ctx, execEnv);

            return execEnv.withTestFileCopyPath(testFileCopyPath);
        });
    }

    public <T> T withTestFileCopied(Path sourceFile, Supplier<T> test) {
        val testFile = setupFileForTesting(sourceFile);
        try {
            return test.get();
        } finally {
            cleanupFileAfterTesting(testFile);
        }
    }

    public Path path() {
        return execEnvPath.toAbsolutePath();
    }

    public Path scriptPath() {
        return testFileCopyPath.toAbsolutePath();
    }

    @Override
    public void close() {
        cleanIfNeeded();
    }

    @SneakyThrows
    private void cleanIfNeeded() {
        if (ctx.clean()) {
            PathUtils.deleteDirectory(execEnvPath);
        }
    }

    @SneakyThrows
    private Path setupFileForTesting(Path sourceFile) {
        var content = Files.readString(sourceFile);
        content = SourceCodeReplacer.replacePlaceholders(content, ctx);
        content = ctx.driver().preprocessScript(ctx, content);

        Files.writeString(testFileCopyPath, content);
        return testFileCopyPath;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }
}
