package com.dtsx.docs.builder;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.drivers.ClientLanguage;
import com.dtsx.docs.lib.Yaml;
import com.dtsx.docs.snapshotters.SnapshotType;
import lombok.NonNull;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.dtsx.docs.lib.Constants.*;

public class TestsBuilder {
    public static Map<TestFixture, Set<TestMetadata>> buildExampleTests(VerifierConfig cfg) {
        val examplesFolder = cfg.examplesFolder();

        if (!Files.exists(examplesFolder) || !Files.isDirectory(examplesFolder)) {
            throw new IllegalStateException("Examples folder does not exist or is not a directory: " + examplesFolder);
        }

        val res = new HashMap<TestFixture, Set<TestMetadata>>();

        try (val stream = Files.list(examplesFolder)) {
            val exampleDirs = stream
                .filter(Files::isDirectory)
                .filter(path -> !path.getFileName().startsWith("_"))
                .toList();

            for (val exampleDir : exampleDirs) {
                buildTestMetadata(exampleDir, cfg).ifPresent((pair) -> {
                    res.computeIfAbsent(pair.getLeft(), _ -> new HashSet<>()).add(pair.getRight());
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list examples folder: " + examplesFolder, e);
        }

        return res;
    }

    private static Optional<Pair<TestFixture, TestMetadata>> buildTestMetadata(Path exampleDir, VerifierConfig cfg) {
        val metaFile = exampleDir.resolve(META_FILE);

        if (!Files.exists(metaFile)) {
            return Optional.empty();
        }

        val meta = Yaml.parse(metaFile, MetaYml.class);

        val exampleFile = findExampleFile(exampleDir, cfg.driver().language());

        if (exampleFile.isEmpty()) {
            return Optional.empty();
        }

        val generalFixture = resolveGeneralFixture(exampleDir.getParent(), meta.fixtures().general());
        val specializedFixture = resolveSpecializedFixture(exampleDir, meta.fixtures().specialized());

        val snapshots = meta.snapshots().orElse(SnapshotsConfig.EMPTY);
        val snapshotTypes = buildSnapshotTypes(snapshots);
        val shareSnapshots = snapshots.share().orElse(true);

        val testMetadata = new TestMetadata(
            exampleDir,
            exampleFile.get(),
            specializedFixture,
            snapshotTypes,
            shareSnapshots
        );

        return Optional.of(Pair.of(generalFixture, testMetadata));
    }

    private static TestFixture resolveGeneralFixture(Path rootDir, String fixtureName) {
        val path = rootDir.resolve(FIXTURES_DIR).resolve(fixtureName);

        if (!Files.exists(path)) {
            throw new IllegalStateException("General fixture does not exist: " + path);
        }

        return new TestFixture(path);
    }

    private static Optional<TestFixture> resolveSpecializedFixture(Path exampleDir, Optional<String> fixtureName) {
        if (fixtureName.isEmpty()) {
            return Files.exists(exampleDir.resolve(DEFAULT_SPECIALIZED_FIXTURE))
                ? Optional.of(new TestFixture(exampleDir.resolve(DEFAULT_SPECIALIZED_FIXTURE)))
                : Optional.empty();
        }

        val path = exampleDir.resolve(fixtureName.get()).resolve(fixtureName.get());

        if (!Files.exists(path)) {
            throw new IllegalStateException("Specialized fixture does not exist: " + path);
        }

        return Optional.of(new TestFixture(path));
    }

    private static TreeSet<SnapshotType> buildSnapshotTypes(SnapshotsConfig config) {
        val types = new TreeSet<SnapshotType>();

        for (val typeName : config.additional().orElse(List.of(SnapshotType.OUTPUT.name()))) {
            try {
                types.add(SnapshotType.valueOf(typeName.toUpperCase()));
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
        @NonNull String general,
        @NonNull Optional<String> specialized
    ) {}

    private record SnapshotsConfig(
        @NonNull Optional<Boolean> share,
        @NonNull Optional<List<String>> additional
    ) {
        public static final SnapshotsConfig EMPTY = new SnapshotsConfig(Optional.empty(), Optional.empty());
    }
}
