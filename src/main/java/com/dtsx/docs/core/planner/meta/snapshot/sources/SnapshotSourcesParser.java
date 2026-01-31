package com.dtsx.docs.core.planner.meta.snapshot.sources;

import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaRep.SnapshotsConfig;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputCaptureSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputJsonifySource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputMatchesSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.records.DocumentsSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.records.RowsSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;
import tools.jackson.core.type.TypeReference;

import java.util.*;
import java.util.function.BiFunction;

public class SnapshotSourcesParser {
    private static final Map<String, BiFunction<String, Map<String, Object>, SnapshotSource>> PARSERS = new HashMap<>() {{
        for (val stream : List.of("stdout", "stderr")) {
            put(stream + "::capture", mk(OutputCaptureSource::new, new TypeReference<>() {}));
            put(stream + "::matches", mk(OutputMatchesSource::new, new TypeReference<>() {}));
            put(stream + "::jsonify", mk(OutputJsonifySource::new, new TypeReference<>() {}));
        }
        put("documents", mk(DocumentsSource::new, new TypeReference<>() {}));
        put("rows", mk(RowsSource::new, new TypeReference<>() {}));
    }};

    public static TreeSet<SnapshotSource> parseSources(SnapshotsConfig config) {
        val sources = new TreeSet<SnapshotSource>();

        for (val rawSource : config.sources().entrySet()) {
            val source = rawSource.getKey();
            val params = Objects.requireNonNullElse(rawSource.getValue(), Collections.<String, Object>emptyMap());

            val parser = PARSERS.get(source);

            if (parser == null) {
                throw new PlanException("Unknown snapshot source type: " + source);
            }

            sources.add(parser.apply(source, params));
        }

        return sources;
    }

    private static <M> BiFunction<String, Map<String, Object>, SnapshotSource> mk(BiFunction<String, M, SnapshotSource> constructor, TypeReference<M> typeRef) {
        return (name, params) -> constructor.apply(name, JacksonUtils.convertValue(params, typeRef));
    }
}
