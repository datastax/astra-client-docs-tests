package com.dtsx.docs.snapshotters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SnapshotType {
    OUTPUT(OutputSnapshotter.INSTANCE),
    COLLECTION(CollectionSnapshotter.INSTANCE),
    TABLE(null),
    COLLECTIONS(null),
    TABLES(null),
    KEYSPACES(null),
    TYPES(null);

    private final Snapshotter snapshotter;
}
