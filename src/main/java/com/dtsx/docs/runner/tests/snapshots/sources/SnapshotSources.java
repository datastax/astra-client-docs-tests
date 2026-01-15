package com.dtsx.docs.runner.tests.snapshots.sources;

import com.dtsx.docs.runner.tests.snapshots.sources.OutputSnapshotSource.*;
import com.dtsx.docs.runner.tests.snapshots.sources.RecordSnapshotSource.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/// Represents the various snapshot sources available in enum form.
///
/// Useful, as `enum`s come with parsing, ordering, and dispatching on static methods for free.
///
/// @see SnapshotSource
@RequiredArgsConstructor
public enum SnapshotSources {
    STDOUT(StdoutSnapshotSource::new, StdoutSnapshotSource::supportedParams),

    STDERR(StderrSnapshotSource::new, StderrSnapshotSource::supportedParams),

    DOCUMENTS(DocumentsSnapshotSource::new, DocumentsSnapshotSource::supportedParams),

    ROWS(RowsSnapshotSource::new, RowsSnapshotSource::supportedParams);

    private final BiFunction<Map<String, Object>, SnapshotSources, SnapshotSource> constructor;
    private final Supplier<List<String>> supportedParams;

    public SnapshotSource create(Map<String, Object> params) {
        return constructor.apply(params, this);
    }

    public List<String> supportedParams() {
        return supportedParams.get();
    }
}
