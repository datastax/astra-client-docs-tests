package com.dtsx.docs.reporter;

import com.dtsx.docs.runner.TestResults;
import lombok.val;

import java.util.Map;
import java.util.function.Supplier;

public interface TestReporter {
    String compileResults(TestResults results);

    static TestReporter parse(String reporter) {
        final Map<String, Supplier<TestReporter>> availableReporters = Map.of(
        );

        val supplier = availableReporters.get(reporter.toLowerCase());

        if (supplier == null) {
            throw new IllegalArgumentException("Unknown reporter: '" + reporter + "' (expected one of: " + String.join(", ", availableReporters.keySet()) + ")");
        }

        return supplier.get();
    }
}
