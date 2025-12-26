package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.builder.fixtures.JSFixtureImpl;
import com.dtsx.docs.builder.fixtures.NoopFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.Yaml;
import com.dtsx.docs.runner.Snapshotter;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.NonNull;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import static com.dtsx.docs.lib.Constants.*;

public class TestPlanBuilder {
    public static TestPlan buildPlan(VerifierCtx ctx) {
        val examplesFolder = ctx.examplesFolder();

        if (!Files.exists(examplesFolder) || !Files.isDirectory(examplesFolder)) {
            throw new IllegalStateException("Examples folder does not exist or is not a directory: " + examplesFolder);
        }

        val plan = new TestPlan();

        try (val stream = Files.list(examplesFolder)) {
            val exampleDirs = stream
                .filter(Files::isDirectory)
                .filter(path -> !path.getFileName().startsWith("_"))
                .toList();

            for (val exampleDir : exampleDirs) {
                buildTestMetadata(exampleDir, ctx).ifPresent(plan::add);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list examples folder: " + examplesFolder, e);
        }

        return plan;
    }

    private static Optional<Pair<JSFixture, TestMetadata>> buildTestMetadata(Path exampleDir, VerifierCtx ctx) {
        val metaFile = exampleDir.resolve(META_FILE);

        if (!Files.exists(metaFile)) {
            return Optional.empty();
        }

        val meta = Yaml.parse(metaFile, MetaYml.class);

        val exampleFile = findExampleFile(exampleDir, ctx.driver().language());

        if (exampleFile.isEmpty()) {
            return Optional.empty();
        }

        val baseFixture = resolveBaseFixture(ctx, exampleDir.getParent(), meta.fixtures().base());
        val testFixture = resolveTestFixture(ctx, exampleDir, meta.fixtures().test());

        val snapshots = meta.snapshots().orElse(SnapshotsConfig.EMPTY);
        val snapshotTypes = buildSnapshotTypes(snapshots);
        val shareSnapshots = snapshots.share().orElse(true);

        val testMetadata = new TestMetadata(
            exampleDir,
            exampleFile.get(),
            testFixture.orElse(NoopFixture.INSTANCE),
            snapshotTypes,
            shareSnapshots
        );

        return Optional.of(Pair.of(baseFixture, testMetadata));
    }

    private static JSFixture resolveBaseFixture(VerifierCtx ctx, Path rootDir, String fixtureName) {
        val path = rootDir.resolve(FIXTURES_DIR).resolve(fixtureName);

        if (!Files.exists(path)) {
            throw new IllegalStateException("Base fixture does not exist: " + path);
        }

        return new JSFixtureImpl(ctx, path);
    }

    private static Optional<JSFixture> resolveTestFixture(VerifierCtx ctx, Path exampleDir, Optional<String> fixtureName) {
        if (fixtureName.isEmpty()) {
            return Files.exists(exampleDir.resolve(DEFAULT_TEST_FIXTURE))
                ? Optional.of(new JSFixtureImpl(ctx, exampleDir.resolve(DEFAULT_TEST_FIXTURE)))
                : Optional.empty();
        }

        val path = exampleDir.resolve(fixtureName.get()).resolve(fixtureName.get());

        if (!Files.exists(path)) {
            throw new IllegalStateException("Test-specific fixture does not exist: " + path);
        }

        return Optional.of(new JSFixtureImpl(ctx, path));
    }

    private static TreeSet<Snapshotter> buildSnapshotTypes(SnapshotsConfig config) {
        val types = new TreeSet<Snapshotter>();

        for (val typeName : config.additional().orElse(List.of(Snapshotter.OUTPUT.name()))) {
            try {
                types.add(Snapshotter.valueOf(typeName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown snapshot type: " + typeName, e);
            }
        }

        return types;
    }

    private static Optional<Path> findExampleFile(Path dir, ClientLanguage lang) {
        try (val paths = Files.walk(dir).skip(1)) {
            for (val path : paths.toList()) {
                if (Files.isDirectory(path)) {
                    val maybeExampleFile = findExampleFile(path, lang);

                    if (maybeExampleFile.isPresent()) {
                        return maybeExampleFile;
                    }
                }

                if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(lang.extension())) {
                    return Optional.of(path);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to detect available drivers in " + dir, e);
        }

        return Optional.empty();
    }

    private record MetaYml(
        @NonNull FixturesConfig fixtures,
        @NonNull Optional<SnapshotsConfig> snapshots
    ) {}

    private record FixturesConfig(
        @NonNull String base,
        @NonNull Optional<String> test
    ) {}

    private record SnapshotsConfig(
        @NonNull Optional<Boolean> share,
        @NonNull Optional<List<String>> additional
    ) {
        public static final SnapshotsConfig EMPTY = new SnapshotsConfig(Optional.empty(), Optional.empty());
    }
}
