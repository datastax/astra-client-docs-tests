package com.dtsx.docs.builder.meta.impls;

import com.dtsx.docs.builder.TestPlanException;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.builder.meta.reps.SnapshotTestMetaYmlRep;
import com.dtsx.docs.builder.meta.reps.SnapshotTestMetaYmlRep.SnapshotsConfig;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.snapshots.sources.SnapshotSource;
import com.dtsx.docs.runner.snapshots.sources.SnapshotSources;
import lombok.Getter;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;

import static com.dtsx.docs.lib.Constants.DEFAULT_TEST_FIXTURE;
import static com.dtsx.docs.lib.Constants.FIXTURES_DIR;

@Getter
public final class SnapshotTestMetaYml implements BaseMetaYml {
    private final JSFixture baseFixture;
    private final JSFixture testFixture;
    private final TreeSet<SnapshotSource> snapshotSources;
    private final boolean shareSnapshots;

    public SnapshotTestMetaYml(VerifierCtx ctx, Path testRoot, SnapshotTestMetaYmlRep meta) {
        this.baseFixture = resolveBaseFixture(ctx, meta.fixtures().base());
        this.testFixture = resolveTestFixture(ctx, testRoot);
        this.snapshotSources = buildSnapshotTypes(meta.snapshots());
        this.shareSnapshots = meta.snapshots().share().orElse(true);
    }

    /// Resolves a base fixture from the `_fixtures/` directory.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   _fixtures/
    ///     basic-collection.js  <- resolves "basic-collection.js"
    /// ```
    ///
    /// @param ctx the verifier context
    /// @param fixtureName the fixture file name from meta.yml
    /// @return the resolved [JSFixture]
    /// @throws TestPlanException if the fixture doesn't exist
    ///
    /// @see JSFixture
    private static JSFixture resolveBaseFixture(VerifierCtx ctx, String fixtureName) {
        val path = ctx.examplesFolder().resolve(FIXTURES_DIR).resolve(fixtureName);

        if (!Files.exists(path)) {
            throw new TestPlanException("Base fixture '" + fixtureName + "' does not exist in '" + FIXTURES_DIR + "'");
        }

        return JSFixture.mkForSnapshotTests(ctx, path);
    }

    /// Resolves a test-specific fixture from the test root directory.
    ///
    /// Looks for `fixture.js` in the test root, if it exists; otherwise returns a no-op fixture.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   dates/
    ///    fixture.js      <- resolves this file
    ///    meta.yml
    ///   delete-many/
    ///    (no fixture.js) <- resolves no-op fixture
    ///    meta.yml
    /// ```
    ///
    /// @param ctx the verifier context
    /// @param testRoot the test root directory
    /// @return the resolved [JSFixture] (or no-op if fixture.js doesn't exist)
    ///
    /// @see JSFixture
    private static JSFixture resolveTestFixture(VerifierCtx ctx, Path testRoot) {
        val path = testRoot.resolve(DEFAULT_TEST_FIXTURE);
        return JSFixture.mkForSnapshotTests(ctx, path);
    }

    /// Builds snapshot sources from the snapshots configuration.
    ///
    /// @param config the snapshots configuration from meta.yml
    /// @return set of configured snapshot sources
    /// @throws TestPlanException if unsupported parameters are provided
    ///
    /// @see SnapshotSources
    private static TreeSet<SnapshotSource> buildSnapshotTypes(SnapshotsConfig config) {
        val sources = new TreeSet<SnapshotSource>();

        for (val rawSource : config.sources().entrySet()) {
            val source = rawSource.getKey();
            val params = Objects.requireNonNullElse(rawSource.getValue(), Collections.<String, Object>emptyMap());

            if (params.keySet().stream().anyMatch(param -> !source.supportedParams().contains(param))) {
                throw new TestPlanException("Unsupported parameter found for snapshot source " + source.name() + ". Supported parameters are: " + String.join(", ", source.supportedParams()));
            }

            sources.add(source.create(params));
        }

        return sources;
    }
}
