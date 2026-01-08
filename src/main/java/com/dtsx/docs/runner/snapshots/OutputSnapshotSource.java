package com.dtsx.docs.runner.snapshots;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;

import java.util.List;
import java.util.Map;

public class OutputSnapshotSource extends SnapshotSource {
    private enum Type {
        ALL,
        STDOUT,
        STDERR
    }

    private Type type = Type.ALL;

    public OutputSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
        super(enumRep);

        if (params.get("capture") != null) {
            try {
                this.type = Type.valueOf(params.get("capture").toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unexpected value for snapshot source output capture: " + params.get("capture") + " (expected one of 'all', 'stdout', 'stderr')");
            }
        }
    }

    @Override
    public String mkSnapshot(VerifierCtx ctx, RunResult res) {
        return switch (type) {
            case ALL -> res.output();
            case STDOUT -> res.stdout();
            case STDERR -> res.stderr();
        };
    }

    public static List<String> supportedParams() {
        return List.of("capture");
    }
}
