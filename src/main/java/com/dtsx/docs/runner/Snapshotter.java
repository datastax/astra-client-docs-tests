package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public enum Snapshotter {
    OUTPUT((_, res) -> {
        return res.output();
    }),

    COLLECTION((cfg, _) -> {
        return Arrays.toString(DataAPIUtils.getCollection(cfg).findAll().stream().toArray());
    }),

    TABLE((cfg, _) -> {
        return Arrays.toString(DataAPIUtils.getTable(cfg).findAll().stream().toArray());
    });

    private final BiFunction<VerifierConfig, RunResult, String> mkSnapshot;

    public String mkSnapshot(VerifierConfig cfg, RunResult res) {
        return mkSnapshot.apply(cfg,  res);
    }
}
