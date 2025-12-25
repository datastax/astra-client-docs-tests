package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
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

    STDOUT((_, res) -> {
        return res.stdout();
    }),

    STDERR((_, res) -> {
        return res.stderr();
    }),

    COLLECTION((ctx, _) -> {
        return Arrays.toString(DataAPIUtils.getCollection(ctx).findAll().stream().toArray());
    }),

    TABLE((ctx, _) -> {
        return Arrays.toString(DataAPIUtils.getTable(ctx).findAll().stream().toArray());
    });

    private final BiFunction<VerifierCtx, RunResult, String> mkSnapshot;

    public String mkSnapshot(VerifierCtx ctx, RunResult res) {
        return mkSnapshot.apply(ctx,  res);
    }
}
