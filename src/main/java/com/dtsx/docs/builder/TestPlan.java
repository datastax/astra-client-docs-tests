package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.*;
import java.util.function.BiConsumer;

import static com.dtsx.docs.lib.ColorUtils.ACCENT_COLOR;
import static com.dtsx.docs.lib.ColorUtils.color;
import static com.dtsx.docs.lib.ColorUtils.highlight;
import static java.util.stream.Collectors.joining;
import static picocli.CommandLine.Help.Ansi.IStyle.CSI;

/// A test plan that groups [TestRoot]s by their base fixture for efficient test execution.
///
/// Test roots sharing the same base fixture are grouped together so the base fixture's
/// `Setup()` and `Teardown()` only need to run once for the entire group.
///
/// Example structure:
/// ```
/// TestPlan:
///   basic-collection.js (base fixture)
///     -> dates/ (test root)
///     -> find-many/ (test root)
///   empty-collection.js (base fixture)
///     -> delete-many/ (test root)
/// ```
///
/// @see TestRoot
/// @see JSFixture
@RequiredArgsConstructor
public class TestPlan {
    private final SequencedMap<JSFixture, SequencedSet<TestRoot>> results;

    /// Iterates over each base fixture and its associated test roots, executing the consumer for each group.
    ///
    /// Example usage:
    /// ```java
    /// plan.forEachBaseFixture((baseFixture, testRoots) -> {
    ///     baseFixture.useResetting(tsx, testRoots, testRoot -> {
    ///         runTests(testRoot);
    ///     });
    /// });
    /// ```
    ///
    /// @param consumer function to execute for each (base fixture, test roots) pair
    public void forEachBaseFixture(BiConsumer<JSFixture, SequencedSet<TestRoot>> consumer) {
        results.forEach(consumer);
    }

    /// Builder for constructing a [TestPlan] by grouping test roots by their base fixture.
    public static class Builder {
        private final SequencedMap<JSFixture, SequencedSet<TestRoot>> results = new LinkedHashMap<>();

        /// Adds a test root to the plan, grouping it with other roots sharing the same base fixture.
        ///
        /// @param pair a (base fixture, test root) pair
        public void addRoot(Pair<JSFixture, TestRoot> pair) {
            results.computeIfAbsent(pair.getLeft(), _ -> new LinkedHashSet<>()).add(pair.getRight());
        }

        /// Returns the total number of example files across all test roots.
        ///
        /// @return total count of example files to test
        public int totalTests() {
            return results.values().stream().flatMap(Set::stream).mapToInt(root -> root.filesToTest().size()).sum();
        }

        /// Builds the final test plan.
        ///
        /// @return the constructed TestPlan
        public TestPlan build() {
            return new TestPlan(results);
        }
    }
}
