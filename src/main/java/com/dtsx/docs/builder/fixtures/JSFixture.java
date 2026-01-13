package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.builder.meta.reps.SnapshotTestMetaYmlRep;
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
import java.util.function.BiConsumer;

import com.dtsx.docs.runner.PlaceholderResolver;
import org.jetbrains.annotations.NotNull;

import static com.dtsx.docs.runner.snapshots.verifier.VerifyMode.DRY_RUN;

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
///    - Referenced in {@link SnapshotTestMetaYmlRep meta.yml} under `fixtures.base`.
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
/// @see SnapshotTestMetaYmlRep
/// @see TestRoot
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public sealed abstract class JSFixture implements Comparable<JSFixture> permits NoopFixture, JSFixtureImpl  {
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
    public abstract FixtureMetadata meta(ExternalProgram tsx);

    // TODO don't know where else to put this but how can we delete all rows in a table without a truncate (like we do for collections)?
    protected abstract void setup(ExternalProgram tsx, FixtureMetadata md);
    protected abstract void reset(ExternalProgram tsx, FixtureMetadata md);
    protected abstract void teardown(ExternalProgram tsx, FixtureMetadata md);

    /// Creates a [JSFixture] for the given path.
    ///
    /// If the path does not exist, or if in dry-run mode, a [NoopFixture] is returned.
    ///
    /// @param ctx  The verifier context.
    /// @param path The path to the JS fixture file.
    /// @return A [JSFixture] instance.
    public static JSFixture mkForSnapshotTests(VerifierCtx ctx, Path path) {
        if (!Files.exists(path)) {
            return NoopFixture.SNAPSHOT_TESTS_INSTANCE;
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
    public <T> void useResetting(ExternalProgram tsx, FixtureMetadata md, Iterable<T> ts, BiConsumer<T, FixtureResetter> consumer) {
        setup(tsx, md);
        try {
            for (val item : ts) {
                reset(tsx, md);
                consumer.accept(item, () -> reset(tsx, md));
            }
        } finally {
            teardown(tsx, md);
        }
    }

    public interface FixtureResetter {
        void reset();
    }

    /// Executes `npm install` in the {@linkplain com.dtsx.docs.runner.ExecutionEnvironment root execution environment folder}
    /// to install any dependencies used by the JS fixtures (e.g. `astra-db-ts`).
    ///
    /// @param ctx the verifier context containing the examples directory path and execution environment path
    /// @throws TestRunException if npm install fails
    public static void installDependencies(VerifierCtx ctx) {
        CliLogger.debug("Installing JSFixture dependencies in " + Path.of(".").toAbsolutePath());

        val res = CliLogger.loading("Installing JS fixture dependencies...", (_) -> {
            return ExternalPrograms.npm(ctx).run("ci", "--prefer-offline"); // TODO use NPM_CONFIG_CACHE in CI
        });

        if (res.exitCode() != 0) {
            throw new TestRunException("Failed to install JS fixture dependencies: " + res.output());
        }
    }

    @Override
    public int compareTo(@NotNull JSFixture o) {
        val p1 = priority(this);
        val p2 = priority(o);

        if (p1 != p2) {
            return Integer.compare(p1, p2);
        }

        return this.fixtureName().compareTo(o.fixtureName());
    }

    // TODO:
    // potentially smarter sorting based on meta() so that fixtures creating heavy resources can be run first (or last??)
    // this would need meta() to be cached through so we aren't re-running JS to get the same metadata multiple times
    // but this might require meta() to be a static cache though in case two different but equal JSFixture instances
    // are created and compared???? can that happen??? Or should JSFixtures themselves be cached and only able to be created
    // through an opaque factory that returns existing instances if they already exist for a given path??? my head hurts.
    private static int priority(JSFixture f) {
        if (f == NoopFixture.COMPILATION_TESTS_INSTANCE) return 0;
        if (f == NoopFixture.SNAPSHOT_TESTS_INSTANCE) return 1;
        return 2;
    }
}
