package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.builder.fixtures.BaseFixture;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.TestResults.TestResult;
import com.dtsx.docs.runner.drivers.ClientDriver;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.file.PathUtils;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.graalvm.collections.Pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class TestRunner {
    private final VerifierConfig cfg;
    private final ExternalProgram tsx;
    private final Map<BaseFixture, Set<TestMetadata>> tests;
    private final ClientDriver driver;

    private TestRunner(VerifierConfig cfg, Map<BaseFixture, Set<TestMetadata>> tests) {
        this.cfg = cfg;
        this.tsx = ExternalPrograms.tsx(cfg);
        this.tests = tests;
        this.driver = cfg.driver();
    }

    public static TestResults runTests(VerifierConfig cfg, Map<BaseFixture, Set<TestMetadata>> tests) {
        return new TestRunner(cfg, tests).runTests();
    }

    private TestResults runTests() {
        val tsx = ExternalPrograms.tsx(cfg);

        val execEnv = setupTempExecutionEnvironment();
        driver.setup(cfg, execEnv);

        val results = new TestResults();

        for (val e : tests.entrySet()) {
            val baseFixture = e.getKey();
            val metadata = e.getValue().stream().toList();

            baseFixture.setup(tsx);

            for (var i = 0; i < metadata.size(); i++) {
                results.add(baseFixture, runTest(metadata.get(i), execEnv));

                if (i < metadata.size() - 1) {
                    baseFixture.reset(tsx);
                }
            }

            baseFixture.teardown(tsx);
        }

        return results;
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

    private TestResults.TestResult runTest(TestMetadata md, Path execEnv) {
        md.testFixture().ifPresent(f -> f.setup(tsx));

        val testFilePath = setupFileForTesting(md.exampleFile(), execEnv);

        val result = driver.execute(cfg, testFilePath);
        val snapshot = mkSnapshot(md, result);
        val res = verifySnapshot(md, snapshot);

        cleanupFileAfterTesting(testFilePath);

        md.testFixture().ifPresent(f -> f.teardown(tsx));
        return res;
    }

    @SneakyThrows
    private Path setupFileForTesting(Path source, Path execEnv) {
        val dest = cfg.driver().testFileCopyDestination(execEnv);

        var content = Files.readString(source);
        content = SourceCodeReplacer.replacePlaceholders(content, cfg);
        content = cfg.driver().preprocessScript(cfg, content);

        Files.writeString(dest, content);
        return dest;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }

    private String mkSnapshot(TestMetadata md, RunResult result) {
        return md.snapshotters().stream()
            .map((snapper) -> Pair.create(snapper, snapper.mkSnapshot(cfg, result)))
            .reduce(
                "",
                (acc, pair) -> acc + "---" + pair.getLeft().name().toLowerCase() + "---\n" + pair.getRight() + "\n",
                (a, b) -> a + b
            );
    }

    private TestResults.TestResult verifySnapshot(TestMetadata md, String snapshot) {
        val namer = new ExampleResultNamer(cfg, md);

        try {
            Approvals.verify(snapshot, new Options().forFile().withNamer(new ExampleResultNamer(cfg, md)));
            return TestResult.passed(md, namer.getExampleName());
        } catch (Error e) {
            return TestResult.failed(md, namer.getExampleName(), e);
        }
    }
}
