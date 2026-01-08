package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.*;
import org.apache.commons.io.file.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ExecutionEnvironment implements AutoCloseable {
    private final VerifierCtx ctx;
    private final Path execEnvPath;

    @With(AccessLevel.PRIVATE)
    private final Path testFileCopyPath;

    public static ExecutionEnvironments setup(VerifierCtx ctx, Collection<ClientDriver> drivers) {
        val res = drivers.stream().collect(Collectors.toMap(
            ClientDriver::language,
            (driver) -> {
                val languageName = driver.language().name().toLowerCase();

                return CliLogger.loading("Setting up @!%s!@ execution environment".formatted(languageName), (_) -> {
                    val srcExecEnv = ctx.executionEnvironmentPathFor(driver.language());
                    val destExecEnv = ctx.tmpFolder().resolve("environments").resolve(driver.language().name().toLowerCase());

                    val execEnv = new ExecutionEnvironment(ctx, destExecEnv, null);
                    execEnv.cleanIfNeeded();

                    try {
                        if (!Files.exists(destExecEnv)) {
                            Files.createDirectories(destExecEnv);
                            PathUtils.copyDirectory(srcExecEnv, destExecEnv);
                        }
                    } catch (Exception e) {
                        throw new TestRunException("Failed to setup " + languageName + " execution environment", e);
                    }

                    val testFileCopyPath = driver.setupExecutionEnvironment(ctx, execEnv);

                    return execEnv.withTestFileCopyPath(testFileCopyPath);
                });
            }
        ));

        return new ExecutionEnvironments(res);
    }

    public <T> T withTestFileCopied(ClientDriver driver, Path sourceFile, Supplier<T> test) {
        val testFile = setupFileForTesting(driver, sourceFile);
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
    private Path setupFileForTesting(ClientDriver driver, Path sourceFile) {
        var content = Files.readString(sourceFile);
        content = SourceCodeReplacer.replacePlaceholders(content, ctx);
        content = driver.preprocessScript(ctx, content);

        Files.writeString(testFileCopyPath, content);
        return testFileCopyPath;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }

    @RequiredArgsConstructor
    public static class ExecutionEnvironments implements AutoCloseable {
        private final Map<ClientLanguage, ExecutionEnvironment> map;

        public ExecutionEnvironment get(ClientLanguage language) {
            return map.get(language);
        }

        @Override
        public void close() {
            map.forEach((_, env) -> env.close());
        }
    }
}
