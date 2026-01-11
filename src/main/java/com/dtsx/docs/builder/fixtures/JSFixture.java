package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.builder.MetaYml;
import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.TestRunException;
import lombok.EqualsAndHashCode;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.dtsx.docs.runner.PlaceholderResolver;
import static com.dtsx.docs.runner.verifier.VerifyMode.DRY_RUN;

/// Represents a JavaScript fixture file used to set up, reset, and tear down database state for testing, with being JavaScript used for ease of scripting.
///
/// JS fixture files are of the following format, where each function is optional:
/// ```javascript
/// import * as $ from '../_base/prelude';
///
/// export async function Setup() {
///   // optional setup code
/// }
///
/// export async function Reset() {
///  // code to reset database state before each test
/// }
///
/// export async function Teardown() {
///  // optional teardown code
/// }
/// ```
///
/// There are two kinds of fixtures:
/// 1. The "base" fixture
///    - This fixture sets up major resources to be reused between a group of tests, such as a collection or keyspace.
///    - Defined in the `_fixtures/` directory of the examples folder.
///    - Referenced in {@link com.dtsx.docs.builder.MetaYml meta.yml} under `fixtures.base`.
/// 2. The "per-test" fixture
///    - This fixture sets up and resets data that is specific to each test, such as rows in a collection.
///    - Defined in the {@link com.dtsx.docs.builder.TestRoot test root directory} of each example.
///    - Always called `fixture.js`
///
/// Example:
/// ```
/// examples/
///   _base/
///     prelude.js
///   _fixtures/
///     basic-collection.js <- base fixture
///   find-many/            <- test root
///     fixture.js          <- per-test fixture
///     meta.yml
/// ```
///
/// @see MetaYml
/// @see TestRoot
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public sealed abstract class JSFixture permits NoopFixture, JSFixtureImpl {
    /// The name of the fixture, derived from its file name (if present)
    ///
    /// Equality is based on this name so that test roots using the same fixture can be grouped together.
    @EqualsAndHashCode.Include
    public abstract String fixtureName();

    /// Returns "metadata" about this fixture, such as names of collections, tables, keyspaces, etc. it creates/works with.
    ///
    /// Expects metadata of the following format to be returned from the fixture file:
    /// ```javascript
    /// export function Meta() {
    ///   return {
    ///     CollectionName?: '...',  // defaults to `null`
    ///     TableName?: '...',       // defaults to `null`
    ///     KeyspaceName?: '...',    // defaults to 'default_keyspace'
    ///   };
    /// }
    /// ```
    ///
    /// The output will be used in {@link PlaceholderResolver} to replace placeholders in code snippets.
    ///
    /// @see PlaceholderResolver
    public abstract FixtureMetadata meta(ExternalProgram tsx, Path nodePath);

    protected abstract void setup(ExternalProgram tsx, Path nodePath, FixtureMetadata md);
    protected abstract void reset(ExternalProgram tsx, Path nodePath, FixtureMetadata md);
    protected abstract void teardown(ExternalProgram tsx, Path nodePath, FixtureMetadata md);

    /// Creates a [JSFixture] for the given path.
    ///
    /// If the path does not exist, or if in dry-run mode, a [NoopFixture] is returned.
    ///
    /// @param ctx  The verifier context.
    /// @param path The path to the JS fixture file.
    /// @return A [JSFixture] instance.
    public static JSFixture mkFor(VerifierCtx ctx, Path path) {
        if (!Files.exists(path)) {
            return NoopFixture.INSTANCE;
        }
        return new JSFixtureImpl(ctx, path, ctx.verifyMode() == DRY_RUN);
    }

    /// Executes a consumer for each item in an iterable, resetting the fixture state before each iteration.
    ///
    /// This method:
    /// 1. Calls `Setup()` once before processing any items
    /// 2. For each item:
    ///    - Calls `Reset()` to restore clean state
    ///    - Executes the consumer with the item
    /// 3. Calls `Teardown()` once after all items are processed (even if an exception occurs)
    ///
    /// Example usage:
    /// ```java
    /// fixture.useResetting(tsx, testRoots, (testRoot) -> {
    ///     // Run test with clean database state
    ///     runTest(testRoot);
    /// });
    /// ```
    ///
    /// @param tsx the TypeScript executor program
    /// @param ts the iterable of items to process
    /// @param consumer the consumer to execute for each item
    public <T> void useResetting(ExternalProgram tsx, Path nodePath, FixtureMetadata md, Iterable<T> ts, Consumer<T> consumer) {
        setup(tsx, nodePath, md);
        try {
            for (val item : ts) {
                reset(tsx, nodePath, md);
                consumer.accept(item);
            }
        } finally {
            teardown(tsx, nodePath, md);
        }
    }

    /// Executes `npm install` in the {@linkplain com.dtsx.docs.runner.ExecutionEnvironment root execution environment folder}
    /// to install any dependencies used by the JS fixtures (e.g. `astra-db-ts`).
    ///
    /// @param ctx the verifier context containing the examples directory path and execution environment path
    /// @throws TestRunException if npm install fails
    public static void installDependencies(VerifierCtx ctx, Path execEnvRoot) {
        CliLogger.debug("Installing JSFixture dependencies in " + execEnvRoot);

        val res = CliLogger.loading("Installing JS fixture dependencies...", (_) -> {
            return ExternalPrograms.npm(ctx).run(execEnvRoot, "install", "@datastax/astra-db-ts");
        });

        if (res.exitCode() != 0) {
            throw new TestRunException("Failed to install JS fixture dependencies: " + res.output());
        }
    }
}
