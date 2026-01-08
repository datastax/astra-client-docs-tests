package com.dtsx.docs.runner.snapshots;

import com.dtsx.docs.runner.snapshots.RecordSnapshotSource.DocumentsSnapshotSource;
import com.dtsx.docs.runner.snapshots.RecordSnapshotSource.RowsSnapshotSource;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@RequiredArgsConstructor
public enum SnapshotSources {
    OUTPUT(OutputSnapshotSource::new, OutputSnapshotSource::supportedParams),

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
