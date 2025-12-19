package com.dtsx.docs.snapshotters;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalRunners.RunResult;
import org.graalvm.collections.Pair;

import java.util.Arrays;

import static com.dtsx.docs.snapshotters.SnapshotType.COLLECTION;

public enum CollectionSnapshotter implements Snapshotter {
    INSTANCE;

    @Override
    public Pair<SnapshotType, String> mkSnapshot(VerifierConfig cfg, RunResult res) {
        return Pair.create(COLLECTION, Arrays.toString(DataAPIUtils.getCollection(cfg).findAll().stream().toArray()));
    }
}
