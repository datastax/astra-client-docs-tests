package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.drivers.ClientDriver;
import lombok.*;
import org.apache.commons.io.file.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

@AllArgsConstructor
@RequiredArgsConstructor
public class ExecutionEnvironment implements AutoCloseable {
    private final VerifierCtx ctx;
    private final Path execEnvPath;

    @With(AccessLevel.PRIVATE)
    private final Path testFileCopyPath;

    @SneakyThrows
    public static ExecutionEnvironment setup(VerifierCtx ctx, ClientDriver driver) {
        val srcExecEnv = ctx.sourceExecutionEnvironment(driver.language());
        val destExecEnv = ctx.tmpFolder().resolve("data/environments").resolve(driver.language().name().toLowerCase());

        val execEnv = new ExecutionEnvironment(ctx, destExecEnv, null);
        execEnv.cleanIfNeeded();

        if (!Files.exists(destExecEnv)) {
            Files.createDirectories(destExecEnv);
            PathUtils.copyDirectory(srcExecEnv, destExecEnv);
        }

        val testFileCopyPath = driver.setupExecutionEnvironment(ctx, execEnv);

        return execEnv.withTestFileCopyPath(testFileCopyPath);
    }

    public <T> T useTestFile(Path sourceFile, Function<Path, T> function) {
        val testFile = setupFileForTesting(sourceFile);
        try {
            return function.apply(testFile);
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
