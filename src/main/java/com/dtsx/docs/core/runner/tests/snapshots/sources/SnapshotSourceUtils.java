package com.dtsx.docs.core.runner.tests.snapshots.sources;

import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.SnapshotVerifier;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class SnapshotSourceUtils {
    public static String extractOutput(String name, RunResult res) { // don't love this but at least it's simple
        if (name.startsWith("stdout")) {
            return res.stdout().trim();
        }
        if (name.startsWith("stderr")) {
            return res.stderr().trim();
        }
        throw new PlanException("Unexpected output stream: '" + name + "'");
    }

    // Sorts records returned by the Data API to ensure deterministic ordering for snapshot comparisons
    //
    // OK to use hash code as a comparator here since all values are "primitive" (or derived from primitives)
    // with strictly defined hash code computations even between different JVMs and runs
    //
    // Any deeper lists would already be returned deterministically by the Data API
    //
    // Any deeper maps will be sorted by SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS
    public static List<?> mkJsonDeterministic(List<?> records) {
        return (List<?>) mkJsonDeterministic((Object) records);
    }

    public static Object mkJsonDeterministic(Object obj) {
        if (obj == null) {
            return null;
        }

        return switch (obj) {
            case Map<?, ?> map -> {
                if (map.size() == 1 && map.containsKey("$binary")) {
                    yield "vector_or_binary";
                }

                var result = new LinkedHashMap<>();
                map.entrySet().stream()
                    .sorted(Comparator.comparing(e -> calcSortValue(e.getKey()), Comparator.nullsFirst(Integer::compareTo)))
                    .forEach(e -> result.put(e.getKey(), mkJsonDeterministic(e.getValue())));
                yield result;
            }
            case Collection<?> coll -> {
                if (coll instanceof List<?> list && !list.isEmpty()) {
                    if (list.stream().allMatch(e -> e instanceof Double)) {
                        yield "vector_or_binary";
                    }
                }

                yield coll.stream()
                    .sorted(Comparator.comparing(SnapshotSourceUtils::calcSortValue, Comparator.nullsFirst(Integer::compareTo)))
                    .map(SnapshotSourceUtils::mkJsonDeterministic)
                    .toList();
            }
            default -> {
                yield obj;
            }
        };
    }

    public static void recursivelyPrintTypes(Object obj, String indent) {
        if (obj instanceof Map<?, ?> map) {
            System.out.println(indent + "Map:");
            for (var entry : map.entrySet()) {
                System.out.print(indent + "  Key (" + entry.getKey().getClass().getSimpleName() + "): ");
                recursivelyPrintTypes(entry.getKey(), indent + "    ");
                System.out.print(indent + "  Value (" + entry.getValue().getClass().getSimpleName() + "): ");
                recursivelyPrintTypes(entry.getValue(), indent + "    ");
            }
        } else if (obj instanceof List<?> list) {
            System.out.println(indent + "List:");
            for (var item : list) {
                System.out.print(indent + "  Item (" + item.getClass().getSimpleName() + "): ");
                recursivelyPrintTypes(item, indent + "    ");
            }
        } else {
            System.out.println(indent + obj.getClass().getSimpleName() + ": " + obj);
        }
    }

    private static int calcSortValue(Object obj) {
        if (obj == null) {
            return 0;
        }

        return switch (obj) {
            case Map<?, ?> map -> {
                yield map.entrySet().stream()
                    .mapToInt(e -> calcSortValue(e.getKey()) ^ calcSortValue(e.getValue()))
                    .sum();
            }
            case List<?> list -> {
                yield list.stream()
                    .mapToInt(SnapshotSourceUtils::calcSortValue)
                    .sum();
            }
            case Set<?> set -> {
                yield set.stream()
                    .mapToInt(SnapshotSourceUtils::calcSortValue)
                    .sum();
            }
            case String str -> {
                if (!SnapshotVerifier.SCRUBBER.scrub(str).equals(str)) {
                    yield 0;
                }
                yield str.hashCode();
            }
            case Long _ -> {
                yield 0; // in case of timestamps
            }
            default -> {
                yield obj.hashCode();
            }
        };
    }
}
