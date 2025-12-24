package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.function.BiConsumer;

public class TestPlan {
    private final SequencedMap<JSFixture, SequencedSet<TestMetadata>> results = new LinkedHashMap<>();

    public void add(Pair<JSFixture, TestMetadata> pair) {
        add(pair.getLeft(), pair.getRight());
    }

    public void add(JSFixture baseFixture, TestMetadata result) {
        results.computeIfAbsent(baseFixture, _ -> new LinkedHashSet<>()).add(result);
    }

    public void forEachBaseFixture(BiConsumer<JSFixture, SequencedSet<TestMetadata>> consumer) {
        results.forEach(consumer);
    }

    public SequencedMap<JSFixture, SequencedSet<TestMetadata>> unwrap() {
        return results;
    }
}
