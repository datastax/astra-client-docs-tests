package com.dtsx.docs.builder;

import com.dtsx.docs.builder.MetaYml.SnapshotsConfig;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.JacksonUtils;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.snapshots.SnapshotSource;
import com.dtsx.docs.runner.snapshots.SnapshotSources;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.dtsx.docs.lib.Constants.*;

public class TestPlanBuilder {
    public static TestPlan buildPlan(VerifierCtx ctx) {
        return CliLogger.loading("Building test plan...", (_) -> {
            val testRoots = findTestRoots(ctx.examplesFolder());

            CliLogger.println("@|faint Building test plan...");
            CliLogger.println("@|faint -> Found " + testRoots.size() + " test roots");

            val plan = new TestPlan(ctx);

            for (val testRoot : testRoots) {
                mkTestRoot(testRoot, ctx).ifPresent(plan::addRoot);
            }

            CliLogger.println("@|faint -> Found " + plan.totalTests() + " example files to test");
            CliLogger.println();

            return plan;
        });
    }

    private static List<Path> findTestRoots(Path examplesFolder) {
        if (!Files.exists(examplesFolder) || !Files.isDirectory(examplesFolder)) {
            throw new TestPlanException("Examples folder '" + examplesFolder + "' does not exist or is not a directory");
        }

        try (val files = Files.walk(examplesFolder)) {
            val dirs = files
                .skip(1)
                .filter(Files::isDirectory)
                .filter(path -> !path.getFileName().toString().startsWith("_"))
                .filter(path -> Files.exists(path.resolve(META_FILE)))
                .toList();

            if (dirs.isEmpty()) {
                throw new TestPlanException("No test roots found in examples directory '" + examplesFolder + "'");
            }

            return dirs;
        } catch (IOException e) {
            throw new TestPlanException("Failed to traverse examples directory '" + examplesFolder + "' to find test roots", e);
        }
    }

    private static Optional<Pair<JSFixture, TestRoot>> mkTestRoot(Path testRoot, VerifierCtx ctx) {
        val meta = parseMetaFile(testRoot.resolve(META_FILE));

        if (meta.skip().orElse(false)) {
            return Optional.empty();
        }

        val filesToTest = findFilesToTestInRoot(testRoot, ctx);

        if (filesToTest.isEmpty()) {
            return Optional.empty();
        }

        val baseFixture = resolveBaseFixture(ctx, meta.fixtures().base());
        val testFixture = resolveTestFixture(ctx, testRoot);

        val snapshotTypes = buildSnapshotTypes(meta.snapshots());
        val shareSnapshots = meta.snapshots().share().orElse(true);

        val testMetadata = new TestRoot(
            testRoot,
            filesToTest,
            testFixture,
            snapshotTypes,
            shareSnapshots
        );

        return Optional.of(Pair.of(baseFixture, testMetadata));
    }

    private static MetaYml parseMetaFile(Path metaFile) {
        try {
            return JacksonUtils.parseYaml(metaFile, MetaYml.class);
        } catch (JacksonException e) {
            throw new TestPlanException("Failed to parse " + metaFile, e);
        }
    }

    private static TreeMap<ClientLanguage, Path> findFilesToTestInRoot(Path root, VerifierCtx ctx) {
        val ret = new TreeMap<ClientLanguage, Path>();

        try (val children = Files.walk(root).skip(1)) {
            children.forEach((child) -> {
                if (!Files.isRegularFile(child)) {
                    return;
                }

                val fileName = child.getFileName().toString().toLowerCase();

                for (val lang : ctx.languages()) {
                    if (fileName.equals("example" + lang.extension()) && ctx.filter().test(child)) {
                        ret.put(lang, child);
                        return;
                    }
                }
            });
        } catch (IOException e) {
            throw new TestPlanException("Failed to traverse test root '" + root + "' to find example files", e);
        }

        return ret;
    }

    private static JSFixture resolveBaseFixture(VerifierCtx ctx, String fixtureName) {
        val path = ctx.examplesFolder().resolve(FIXTURES_DIR).resolve(fixtureName);

        if (!Files.exists(path)) {
            throw new TestPlanException("Base fixture '" + fixtureName + "' does not exist in '" + FIXTURES_DIR + "'");
        }

        return JSFixture.mkFor(ctx, path);
    }

    private static JSFixture resolveTestFixture(VerifierCtx ctx, Path testRoot) {
        val path = testRoot.resolve(DEFAULT_TEST_FIXTURE);
        return JSFixture.mkFor(ctx, path);
    }

    private static TreeSet<SnapshotSource> buildSnapshotTypes(SnapshotsConfig config) {
        val sources = new TreeSet<SnapshotSource>();

        for (val rawSource : config.sources().entrySet()) {
            val source = SnapshotSources.valueOf(rawSource.getKey().toUpperCase());
            val params = Objects.requireNonNullElse(rawSource.getValue(), Collections.<String, Object>emptyMap());

            if (params.keySet().stream().anyMatch(param -> !source.supportedParams().contains(param))) {
                throw new TestPlanException("Unsupported parameter found for snapshot source " + source.name() + ". Supported parameters are: " + String.join(", ", source.supportedParams()));
            }

            sources.add(source.create(params));
        }

        return sources;
    }
}
