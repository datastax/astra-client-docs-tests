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

@RequiredArgsConstructor
public class TestPlan {
    private final VerifierCtx ctx;
    private final SequencedMap<JSFixture, SequencedSet<TestRoot>> results = new LinkedHashMap<>();

    public void addRoot(Pair<JSFixture, TestRoot> pair) {
        results.computeIfAbsent(pair.getLeft(), _ -> new LinkedHashSet<>()).add(pair.getRight());
    }

    public void forEachBaseFixture(BiConsumer<JSFixture, SequencedSet<TestRoot>> consumer) {
        results.forEach(consumer);
    }

    public SequencedMap<JSFixture, SequencedSet<TestRoot>> unwrap() {
        return results;
    }

    public int totalRoots() {
        return results.values().stream().mapToInt(Set::size).sum();
    }

    public int totalTests() {
        return results.values().stream().flatMap(Set::stream).mapToInt(root -> root.filesToTest().size()).sum();
    }
}
