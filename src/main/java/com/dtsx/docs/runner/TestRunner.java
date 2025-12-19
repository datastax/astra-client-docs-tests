package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.builder.TestFixture;
import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.drivers.ClientDriver;
import com.dtsx.docs.lib.ExternalRunners;
import com.dtsx.docs.lib.ExternalRunners.ExternalRunner;
import com.dtsx.docs.lib.ExternalRunners.RunResult;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.file.PathUtils;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class TestRunner {
    private final VerifierConfig cfg;
    private final ExternalRunner tsx;
    private final Map<TestFixture, Set<TestMetadata>> tests;
    private final ClientDriver driver;

    private TestRunner(VerifierConfig cfg, Map<TestFixture, Set<TestMetadata>> tests) {
        this.cfg = cfg;
        this.tsx = ExternalRunners.tsx(cfg);
        this.tests = tests;
        this.driver = cfg.driver();
    }

    public static void runTests(VerifierConfig cfg, Map<TestFixture, Set<TestMetadata>> tests) {
        new TestRunner(cfg, tests).runTests();
    }

    private void runTests() {
        val tsx = ExternalRunners.tsx(cfg);

        val execEnv = setupTempExecutionEnvironment();
        driver.beforeAll(cfg, execEnv);

        for (val e : tests.entrySet()) {
            val fixture = e.getKey();
            val metadata = e.getValue().stream().toList();

            fixture.setup(tsx);

            for (var i = 0; i < metadata.size(); i++) {
                runTest(metadata.get(i), execEnv);

                if (i < metadata.size() - 1) {
                    fixture.reset(tsx);
                }
            }

            fixture.teardown(tsx);
        }
    }

    @SneakyThrows
    private Path setupTempExecutionEnvironment() {
        val srcExecEnv = cfg.sourceExecutionEnvironment(driver.language());
        val destExecEnv = cfg.tmpFolder().resolve("environments").resolve(driver.language().name().toLowerCase());

        if (cfg.clean()) {
            PathUtils.deleteDirectory(destExecEnv);
        }

        if (!Files.exists(destExecEnv)) {
            Files.createDirectories(destExecEnv);
            PathUtils.copyDirectory(srcExecEnv, destExecEnv);
        }

        return destExecEnv;
    }

    private void runTest(TestMetadata md, Path execEnv) {
        md.specializedFixture().ifPresent(f -> f.setup(tsx));

        val testFilePath = setupFileForTesting(md.exampleFile(), execEnv);

        val result = driver.execute(cfg, testFilePath);

        val snapshot = mkSnapshot(md, result);
        verifySnapshot(md, snapshot);

        cleanupFileAfterTesting(testFilePath);

        md.specializedFixture().ifPresent(f -> f.teardown(tsx));
    }

    @SneakyThrows
    private Path setupFileForTesting(Path source, Path execEnv) {
        val content = Files.readString(source);

        val dest = cfg.driver().executionLocation(execEnv);

        Files.writeString(dest, SourceCodeReplacer.replacePlaceholders(content, cfg));
        return dest;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }

    private String mkSnapshot(TestMetadata md, RunResult result) {
        return md.snapshotTypes().stream()
            .map((t) -> t.snapshotter().mkSnapshot(cfg, result))
            .reduce(
                "",
                (acc, snap) -> acc + "---" + snap.getLeft().name().toLowerCase() + "---\n" + snap.getRight() + "\n",
                (a, b) -> a + b
            );
    }

    private void verifySnapshot(TestMetadata md, String snapshot) {
        Approvals.verify(snapshot, new Options().forFile().withNamer(new ExampleResultNamer(cfg, md)));
    }
}
