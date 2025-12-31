package com.dtsx.docs.builder;

import com.dtsx.docs.builder.fixtures.JSFixture;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiConsumer;

public class TestPlan {
    private final SequencedMap<JSFixture, SequencedSet<TestMetadata>> results = new LinkedHashMap<>();

    public void add(Pair<JSFixture, TestMetadata> pair) {
        results.computeIfAbsent(pair.getLeft(), _ -> new LinkedHashSet<>()).add(pair.getRight());
    }

    public void forEachBaseFixture(BiConsumer<JSFixture, SequencedSet<TestMetadata>> consumer) {
        results.forEach(consumer);
    }

    public SequencedMap<JSFixture, SequencedSet<TestMetadata>> unwrap() {
        return results;
    }

    @Override
    public String toString() {
        val sb = new StringBuilder();

        val fixtureIndex = new Object() {
            int ref = 1;
        };

        results.forEach((fixture, tests) -> {
            sb.append(fixtureIndex.ref++)
                .append(") ")
                .append(fixture.fixtureName())
                .append("\n");

            tests.forEach((test) ->
                test.exampleFiles().forEach((exampleFile) ->
                    sb.append("  - ")
                        .append(test.exampleFolder().getFileName())
                        .append("/")
                        .append(test.exampleFolder().relativize(exampleFile.getRight()))
                        .append("\n")));
        });

        return sb.toString();
    }
}
