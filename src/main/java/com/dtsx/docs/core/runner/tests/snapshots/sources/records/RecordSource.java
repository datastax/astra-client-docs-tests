package com.dtsx.docs.core.runner.tests.snapshots.sources.records;

import com.datastax.astra.client.core.query.Filter;
import com.datastax.astra.client.core.query.Projection;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaRep;
import com.dtsx.docs.core.planner.meta.snapshot.sources.RecordSourceMeta;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputCaptureSource;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;

/// Base class for snapshot sources that deterministically captures database records (documents or rows).
///
/// Implemented by [DocumentsSource] and [RowsSource].
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
/// @apiNote Pairs well with [OutputCaptureSource] to capture any undesired warnings or errors
///
/// @see SnapshotTestMetaRep
public abstract class RecordSource extends SnapshotSource {
    protected final Optional<Filter> filter;
    protected final Optional<Projection[]> projection;

    public RecordSource(String name, RecordSourceMeta meta) {
        super(name);
        this.filter = meta.filter().map(Filter::new);
        this.projection = meta.projection().map(this::buildProjection);
    }

    private Projection[] buildProjection(Map<String, Object> projectionMap) {
        val projections = new ArrayList<Projection>();

        for (val entry : projectionMap.entrySet()) {
            val field = entry.getKey();
            val value = entry.getValue();

            if (value.equals(1) || value.equals(true)) {
                projections.add(Projection.include(field)[0]);
            } else if (value.equals(0) || value.equals(false)) {
                projections.add(Projection.exclude(field)[0]);
            } else if (value instanceof Map<?, ?> map) {
                val sliceValue = map.get("$slice");

                switch (sliceValue) {
                    case null -> {
                        throw new PlanException("The projection operator map for field '" + field + "' must contain a '$slice' key");
                    }
                    case Integer start -> {
                        projections.add(Projection.slice(field, start, null));
                    }
                    case List<?> list when list.size() == 2 -> {
                        if (list.get(0) instanceof Integer start && list.get(1) instanceof Integer end) {
                            projections.add(Projection.slice(field, start, end));
                        } else {
                            throw new PlanException("The '$slice' values must be integers");
                        }
                    }
                    default -> {
                        throw new PlanException("The '$slice' value must be an integer or a list of two integers");
                    }
                }
            } else {
                throw new PlanException("The projection value for field '" + field + "' must be 1, 0, true, false, or a valid $slice map");
            }
        }

        return projections.toArray(new Projection[0]);
    }

    protected abstract Optional<String> extractSchemaObjectName(Placeholders placeholders);
    protected abstract Stream<Map<String, Object>> streamRecords(TestCtx ctx, String name);

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        // error should never be thrown since it would've been caught earlier in PlaceholderResolver.resolvePlaceholders
        // since the snapshot shouldn't be depending on a collection/table that the example file doesn't explicitly use anyway
        val schemaObjName = extractSchemaObjectName(placeholders).orElseThrow(() -> new PlanException("Could not determine schema object name from fixture metadata"));

        return JacksonUtils.prettyPrintJson(
            mkJsonDeterministic(streamRecords(ctx, schemaObjName).toList())
        );
    }
}
