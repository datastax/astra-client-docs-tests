package com.dtsx.docs.core.runner.tests.snapshots.sources;

import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.query.Filter;
import com.datastax.astra.client.core.query.Projection;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.reps.SnapshotTestMetaYmlRep;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.SnapshotVerifier;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/// Base class for snapshot sources that deterministically captures database records (documents or rows).
///
/// Implemented by [DocumentsSnapshotSource] and [RowsSnapshotSource].
///
/// Records are sorted to ensure deterministic ordering for snapshot comparisons, even with dynamically generated IDs or timestamps.
///
/// Supports an optional collection/table filter to narrow the records captured in the snapshot.
/// Supports an optional projection to include/exclude specific fields from the snapshot.
///
/// Example configuration:
/// ```
/// documents:
///   filter: { "status": "active" } <- optional Data API filter
///   projection: { "name": 1, "email": 1 } <- optional Data API projection (include fields)
/// ```
/// or
/// ```
/// documents:
///   filter: { "status": "active" }
///   projection: { "password": 0, "ssn": 0 } <- optional Data API projection (exclude fields)
/// ```
///
/// @apiNote Pairs well with [OutputSnapshotSource] to capture any undesired warnings or errors
///
/// @see SnapshotSources
/// @see SnapshotTestMetaYmlRep
public sealed abstract class RecordSnapshotSource extends SnapshotSource {
    protected @Nullable Filter filter;
    protected @Nullable Projection[] projection;

    @SuppressWarnings("unchecked")
    public RecordSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
        super(enumRep);

        if (params.get("filter") != null) {
            if (params.get("filter") instanceof Map<?, ?> filterMap) {
                this.filter = new Filter((Map<String, Object>) filterMap);
            } else {
                throw new PlanException("The 'filter' parameter must be a Map<String, Object>");
            }
        }

        if (params.get("projection") != null) {
            if (params.get("projection") instanceof Map<?, ?> projectionMap) {
                this.projection = buildProjection((Map<String, Object>) projectionMap);
            } else {
                throw new PlanException("The 'projection' parameter must be a Map<String, Object>");
            }
        }
    }

    private Projection[] buildProjection(Map<String, Object> projectionMap) {
        // Determine if this is an include or exclude projection
        // We need to check ALL fields (including _id) to determine the projection type
        Boolean isInclude = null;

        for (Map.Entry<String, Object> entry : projectionMap.entrySet()) {
            Object value = entry.getValue();

            // Determine if this is include (1/true) or exclude (0/false)
            boolean fieldIsInclude = value.equals(1) || value.equals(true) || value.equals(1L);

            if (isInclude == null) {
                isInclude = fieldIsInclude;
            } else if (isInclude != fieldIsInclude) {
                // Only _id and $ fields can be mixed with other projections
                String field = entry.getKey();
                if (!field.equals("_id") && !field.startsWith("$")) {
                    throw new PlanException("Cannot mix include and exclude projections (except for _id and $ fields)");
                }
            }
        }

        // This should never be null since we have at least one entry
        if (isInclude == null) {
            throw new PlanException("Projection map is empty");
        }

        final boolean finalIsInclude = isInclude;
        String[] fields = projectionMap.keySet().toArray(new String[0]);

        if (finalIsInclude) {
            return Projection.include(fields);
        } else {
            return Projection.exclude(fields);
        }
    }

    protected abstract Optional<String> extractSchemaObjectName(Placeholders placeholders);
    protected abstract Stream<Map<String, Object>> streamRecords(TestCtx ctx, String name);

    @Override
    public String mkSnapshot(TestCtx ctx, RunResult res, Placeholders placeholders) {
        // error should never be thrown since it would've been caught earlier in PlaceholderResolver.resolvePlaceholders
        // since the snapshot shouldn't be depending on a collection/table that the example file doesn't explicitly use anyway
        val schemaObjName = extractSchemaObjectName(placeholders).orElseThrow(() -> new PlanException("Could not determine schema object name from fixture metadata"));

        return JacksonUtils.prettyPrintJson(
            mkJsonDeterministic(streamRecords(ctx, schemaObjName).toList())
        );
    }

    public static List<String> supportedParams() {
        return List.of("filter", "projection");
    }

    /// Implementation of [RecordSnapshotSource] that captures documents from a collection.
    public static final class DocumentsSnapshotSource extends RecordSnapshotSource {
        public DocumentsSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
            super(params, enumRep);
        }

        @Override
        protected Optional<String> extractSchemaObjectName(Placeholders placeholders) {
            return placeholders.collectionName();
        }

        @Override
        public Stream<Map<String, Object>> streamRecords(TestCtx ctx, String name) {
            var collection = DataAPIUtils.getCollection(ctx.connectionInfo(), name);
            if (projection != null) {
                return collection.find(filter, new com.datastax.astra.client.collections.commands.options.CollectionFindOptions().projection(projection))
                    .stream()
                    .map(Document::getDocumentMap);
            }
            return collection.find(filter).stream().map(Document::getDocumentMap);
        }
    }

    /// Implementation of [RecordSnapshotSource] that captures rows from a table.
    public static final class RowsSnapshotSource extends RecordSnapshotSource {
        public RowsSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
            super(params, enumRep);
        }

        @Override
        protected Optional<String> extractSchemaObjectName(Placeholders placeholders) {
            return placeholders.tableName();
        }

        @Override
        public Stream<Map<String, Object>> streamRecords(TestCtx ctx, String name) {
            var table = DataAPIUtils.getTable(ctx.connectionInfo(), name);
            if (projection != null) {
                return table.find(filter, new com.datastax.astra.client.tables.commands.options.TableFindOptions().projection(projection))
                    .stream()
                    .map(Row::getColumnMap);
            }
            return table.find(filter).stream().map(Row::getColumnMap);
        }
    }

    // Sorts records returned by the Data API to ensure deterministic ordering for snapshot comparisons
    //
    // OK to use hash code as a comparator here since all values are "primitive" (or derived from primitives)
    // with strictly defined hash code computations even between different JVMs and runs
    //
    // Any deeper lists would already be returned deterministically by the Data API
    //
    // Any deeper maps will be sorted by SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS
    protected List<?> mkJsonDeterministic(List<?> records) {
        return records.stream()
            .sorted(Comparator.comparing(this::calcSortValue))
            .toList();
    }

    private int calcSortValue(Object obj) {
        return switch (obj) {
            case Map<?, ?> map -> {
                yield map.entrySet().stream()
                    .mapToInt(e -> calcSortValue(e.getKey()) ^ calcSortValue(e.getValue()))
                    .sum();
            }
            case List<?> list -> {
                yield list.stream()
                    .mapToInt(this::calcSortValue)
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

    private void recursivelyPrintTypes(Object obj, String indent) {
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
