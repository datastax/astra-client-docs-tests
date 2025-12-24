package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.builder.fixtures.JSFixture;

import java.util.*;

public class TestResults {
    private final Map<JSFixture, List<TestResult>> results = new HashMap<>();

    public void add(JSFixture baseFixture, TestResult result) {
        results.computeIfAbsent(baseFixture, _ -> new ArrayList<>()).add(result);
    }

    public Map<JSFixture, List<TestResult>> unwrap() {
        return results;
    }

    public record TestResult(TestMetadata metadata, String snapshotFile, Optional<Error> error) {
        public static TestResult passed(TestMetadata metadata, String snapshotFile) {
            return new TestResult(metadata, snapshotFile, Optional.empty());
        }

        public static TestResult failed(TestMetadata metadata, String snapshotFile, Error error) {
            return new TestResult(metadata, snapshotFile, Optional.of(error));
        }

        public boolean approved() {
            return error.isEmpty();
        }
    }
}
