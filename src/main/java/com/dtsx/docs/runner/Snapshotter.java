package com.dtsx.docs.runner;

import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public enum Snapshotter {
    OUTPUT((_, res) -> {
        return res.output();
    }),

    STDOUT((_, res) -> {
        return res.stdout();
    }),

    STDERR((_, res) -> {
        return res.stderr();
    }),

    COLLECTION((ctx, _) -> {
        return JacksonUtils.prettyPrintJson(
            mkJsonDeterministic(DataAPIUtils.getCollection(ctx).findAll().stream().map(Document::getDocumentMap).toList())
        );
    }),

    TABLE((ctx, _) -> {
        return JacksonUtils.prettyPrintJson(
            mkJsonDeterministic(DataAPIUtils.getTable(ctx).findAll().stream().map(Row::getColumnMap).toList())
        );
    });

    private final BiFunction<VerifierCtx, RunResult, String> mkSnapshot;

    public String mkSnapshot(VerifierCtx ctx, RunResult res) {
        return mkSnapshot.apply(ctx,  res);
    }

    // Sorts records returned by the Data API to ensure deterministic ordering for snapshot comparisons
    //
    // OK to use hash code as a comparator here since all values are "primitive" (or derived from primitives)
    // with strictly defined hash code computations even between different JVMs and runs
    //
    // Any deeper lists would already be returned deterministically by the Data API
    //
    // Any deeper maps will be sorted by SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS
    private static List<?> mkJsonDeterministic(List<?> records) {
        return records.stream()
            .sorted(Comparator.comparing(Snapshotter::calcSortValue))
            .toList();
    }

    private static int calcSortValue(Object obj) {
        return switch (obj) {
            case Map<?, ?> map -> {
                yield map.entrySet().stream()
                    .mapToInt(e -> calcSortValue(e.getKey()) ^ calcSortValue(e.getValue()))
                    .sum();
            }
            case List<?> list -> {
                yield list.stream()
                    .mapToInt(Snapshotter::calcSortValue)
                    .sum();
            }
            case String str -> {
                if (!TestVerifier.SCRUBBER.scrub(str).equals(str)) {
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

    private static void recursivelyPrintTypes(Object obj, String indent) {
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
}
