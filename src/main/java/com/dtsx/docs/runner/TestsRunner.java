package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.builder.TestFixture;
import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.drivers.ClientLanguage;
import com.dtsx.docs.lib.ExternalRunners;
import com.dtsx.docs.lib.ExternalRunners.ExternalRunner;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class TestsRunner {
    private final VerifierConfig cfg;
    private final ExternalRunner tsx;
    private final Map<TestFixture, Set<TestMetadata>> tests;
    private final ClientLanguage lang;

    private TestsRunner(VerifierConfig cfg, Map<TestFixture, Set<TestMetadata>> tests) {
        this.cfg = cfg;
        this.tsx = ExternalRunners.tsx(cfg);
        this.tests = tests;
        this.lang = cfg.driver().language();
    }

    public static void runTests(VerifierConfig cfg, Map<TestFixture, Set<TestMetadata>> tests) {
        new TestsRunner(cfg, tests).runTests();
    }

    private void runTests() {
        val tsx = ExternalRunners.tsx(cfg);

        for (val e : tests.entrySet()) {
            val fixture = e.getKey();
            val metadata = e.getValue().stream().toList();

            fixture.setup(tsx);

            for (var i = 0; i < metadata.size(); i++) {
                runTest(metadata.get(i));

                if (i < metadata.size() - 1) {
                    fixture.reset(tsx);
                }
            }

            fixture.teardown(tsx);
        }
    }

    private void runTest(TestMetadata md) {
        md.specializedFixture().ifPresent(f -> f.setup(tsx));

        val testFilePath = setupFileForTesting(md.exampleFile());

        cleanupFileAfterTesting(testFilePath);

        md.specializedFixture().ifPresent(f -> f.teardown(tsx));
    }

    @SneakyThrows
    private Path setupFileForTesting(Path source) {
        val execEnv = cfg.executionEnvironment(lang);

        val dest = cfg.driver().executionLocation(execEnv);

        val content = Files.readString(source)
            .replace("oldText", "newText");

        Files.writeString(dest, content);

        return dest;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }
}
