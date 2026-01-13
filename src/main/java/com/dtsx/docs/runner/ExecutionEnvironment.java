package com.dtsx.docs.runner;

import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.builder.fixtures.JSFixture;
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

/// An isolated execution environment for running example code in a specific client language.
///
/// Each environment is a temporary directory containing:
/// - Language-specific project structure (e.g., `build.gradle` for Java, `package.json` for TypeScript)
/// - Installed dependencies
/// - A location for the test file to be copied and executed
///
/// Example structure:
/// ```text
/// .docs_tests_temp/execution_environments/
///   typescript/
///     package.json
///     node_modules/
///     example.ts      <- test file copied here
///   java/
///     build.gradle
///     src/main/java/
///       Example.java  <- test file copied here
/// ```
///
/// The environment handles:
/// - Dependency installation
/// - Copying and pre-processing test files based on the {@linkplain ClientDriver client language}
/// - Cleanup after test execution
///
/// The files themselves are derived from a template directory at `./resources/environments/` containing
/// the base structure for each language.
///
/// @see ExecutionEnvironments
/// @see ClientDriver
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionEnvironment {
    private final VerifierCtx ctx;
    private final Path execEnvPath;

    @With(AccessLevel.PRIVATE)
    private final Path testFileCopyPath;

    /// Does what it says on the tin ([ExecutionEnvironment])
    ///
    /// Example usage:
    /// ```java
    /// try (val execEnvs = ExecutionEnvironment.setup(ctx, drivers)) {
    ///     useItHere(execEnvs);
    /// }
    /// ```
    ///
    /// @param ctx the verifier context
    /// @param drivers the client drivers to create environments for
    /// @return a collection of execution environments, one per language
    public static ExecutionEnvironments setup(VerifierCtx ctx, Collection<ClientDriver> drivers) {
        return Builder.setup(ctx, drivers);
    }

    /// Copies a test file into the execution environment, runs the test, then cleans up.
    ///
    /// The test file is preprocessed to replace placeholders (e.g., `{{API_ENDPOINT}}`)
    /// and apply language-specific transformations before execution.
    ///
    /// Example usage:
    /// ```java
    /// val result = execEnv.withTestFileCopied(driver, sourceFile, () -> {
    ///     return insertTestLogicHere();
    /// });
    /// ```
    ///
    /// @param driver the client driver for the test
    /// @param sourceFile the original test file to copy
    /// @param test the test to run with the copied file
    /// @return the result of the test execution
    public <T> T withTestFileCopied(ClientDriver driver, Path sourceFile, FixtureMetadata md, Supplier<T> test) {
        val testFile = setupFileForTesting(driver, sourceFile, md);
        try {
            return test.get();
        } finally {
            cleanupFileAfterTesting(testFile);
        }
    }

    /// Returns the absolute path to the language's execution environment directory.
    ///
    /// @return the environment directory path
    public Path envDir() {
        return execEnvPath.toAbsolutePath();
    }

    /// Returns the absolute path to the copied test script file.
    ///
    /// @return the test script path
    public String scriptPath() {
        return testFileCopyPath.toAbsolutePath().toString();
    }

    /// A collection of execution environments, one for each client language.
    ///
    /// Provides access to environments by language and handles any cleanup of all environments if ran with the `--clean` flag.
    ///
    /// @see ExecutionEnvironment
    @RequiredArgsConstructor
    public static class ExecutionEnvironments {
        private final Path rootDir;
        private final Map<ClientLanguage, ExecutionEnvironment> map;

        /// Gets the execution environment for a specific language.
        ///
        /// @param language the client language
        /// @return the execution environment for that language
        public ExecutionEnvironment get(ClientLanguage language) {
            return map.get(language);
        }

        /// Returns the absolute path to the `node_modules` directory within the execution environment.
        ///
        /// @return the `node_modules` path
        public Path nodePath() {
            return rootDir.resolve("node_modules").toAbsolutePath();
        }
    }

    @SneakyThrows
    private Path setupFileForTesting(ClientDriver driver, Path sourceFile, FixtureMetadata md) {
        var content = Files.readString(sourceFile);
        content = PlaceholderResolver.replacePlaceholders(ctx, md, content);
        content = driver.preprocessScript(ctx, content);

        Files.createDirectories(testFileCopyPath.getParent());
        Files.writeString(testFileCopyPath, content);
        return testFileCopyPath;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }

    /// Internal builder for setting up execution environments.
    ///
    /// Setup process:
    /// 1. Creates root folder at `.docs_tests_temp/execution_environments/`
    /// 2. Installs JavaScript dependencies (for fixtures)
    ///    - Creating `package.json` if necessary
    /// 3. For each client language:
    ///    - Copies template from `resources/environments/<language>/`
    ///    - Lets the driver set up language-specific dependencies
    ///    - Determines where test files will be copied
    private static class Builder {
        private static ExecutionEnvironments setup(VerifierCtx ctx, Collection<ClientDriver> drivers) {
            val rootDir = mkRootFolder(ctx);
            installJsDependencies(ctx, rootDir);
            return mkExecEnvs(ctx, drivers, rootDir);
        }

        private static Path mkRootFolder(VerifierCtx ctx) {
            val rootFolder = ctx.tmpFolder().resolve("execution_environments");

            try {
                Files.createDirectories(rootFolder);
                return rootFolder;
            } catch (Exception e) {
                throw new TestRunException("Failed to create execution environments root folder", e);
            }
        }

        private static void installJsDependencies(VerifierCtx ctx, Path execEnvRoot) {
            try {
                val packageJson = execEnvRoot.resolve("package.json");

                if (!Files.exists(packageJson)) {
                    Files.writeString(packageJson, "{}");
                }

                JSFixture.installDependencies(ctx, execEnvRoot);
            } catch (Exception e) {
                throw new TestRunException("Failed to setup JS dependencies", e);
            }
        }

        private static ExecutionEnvironments mkExecEnvs(VerifierCtx ctx, Collection<ClientDriver> drivers, Path rootDir) {
            val execEnvs = drivers.stream().collect(Collectors.toMap(
                ClientDriver::language,
                (driver) -> mkExecEnv(ctx, rootDir, driver)
            ));

            return new ExecutionEnvironments(rootDir, execEnvs);
        }

        private static ExecutionEnvironment mkExecEnv(VerifierCtx ctx, Path rootDir, ClientDriver driver) {
            val languageName = driver.language().name().toLowerCase();

            return CliLogger.loading("Setting up @!%s!@ execution environment".formatted(languageName), (_) -> {
                val srcExecEnv = ctx.executionEnvironmentTemplate(driver.language());
                val destExecEnv = rootDir.resolve(driver.language().name().toLowerCase());

                val execEnv = new ExecutionEnvironment(ctx, destExecEnv, null);
                cleanIfNeeded(ctx, destExecEnv);

                try {
                    if (!Files.exists(destExecEnv)) {
                        CliLogger.debug("Setting up %s execution environment from template".formatted(languageName));
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

        @SneakyThrows
        private static void cleanIfNeeded(VerifierCtx ctx, Path execEnv) {
            if (ctx.clean()) {
                CliLogger.debug("Cleaning up execution environments");
                PathUtils.deleteDirectory(execEnv);
            }
        }
    }
}
