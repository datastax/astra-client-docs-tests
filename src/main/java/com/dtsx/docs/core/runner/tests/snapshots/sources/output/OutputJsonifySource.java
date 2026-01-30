package com.dtsx.docs.core.runner.tests.snapshots.sources.output;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.sources.OutputJsonifySourceMeta;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.SnapshotVerifier.$DateScrubber;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.regex.Pattern;

import static com.dtsx.docs.lib.JacksonUtils.runJq;

public class OutputJsonifySource extends SnapshotSource {
    private final OutputJsonifySourceMeta meta;

    public OutputJsonifySource(String name, OutputJsonifySourceMeta meta) {
        super(name);
        this.meta = meta;
    }

    @Override
    public String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders) {
        val output = SnapshotSourceUtils.extractOutput(name, res);
        val rawJson = driver.preprocessToJson(ctx, meta, output);

        var jsonAsString = JacksonUtils.printJson(rawJson);
        jsonAsString = JsonScrubber.scrub(jsonAsString);

        if (meta.jq().isPresent()) {
            jsonAsString = runJq(ctx, meta.jq().get(), jsonAsString);
        }

        val processJson = JacksonUtils.parseJson(jsonAsString, List.class);

        return JacksonUtils.prettyPrintJson(
            SnapshotSourceUtils.mkJsonDeterministic(processJson)
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static final class JsonScrubber {
        private static final Pattern BINARY_PATTERN =
            Pattern.compile("\\{\"\\$binary\":\".*?\"}");

        private static final Pattern VECTOR_PATTERN =
            Pattern.compile("\\[(?:\\d*?\\.\\d+,)*\\d*\\.\\d+]");

        public static final Pattern VECTOR_OBJECT_PATTERN =
            Pattern.compile("\\{\"_vector\":\\[.*?]}");

        private static final Pattern RFC_DATE_PATTERN =
            Pattern.compile("\"\\b\\d{4}-\\d{2}-\\d{2}\\b\"");

        public static final Pattern DATE_OBJECT_PATTERN =
            Pattern.compile("\\{\"date\":\\d+?,\"month\":\\d+?,\"year\":\\d+?}");

        private static final Pattern RFC_TIME_PATTERN =
            Pattern.compile("\"\\b\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?\"");

        public static final Pattern TIME_OBJECT_PATTERN =
            Pattern.compile("\\{\"hours\":\\d+?,\"minutes\":\\d+?(,\"nanoseconds\":\\d+?)?,\"seconds\":\\d+?}");

        private static final Pattern RFC_TIMESTAMP_PATTERN =
            Pattern.compile("\"\\b\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})\\b\"");

        private static String scrubVectorOrBinary(String json, String replacement) {
            json = VECTOR_OBJECT_PATTERN.matcher(json).replaceAll(replacement);
            json = BINARY_PATTERN.matcher(json).replaceAll(replacement);
            json = VECTOR_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        private static String scrubDate(String json, String replacement) {
            json = RFC_DATE_PATTERN.matcher(json).replaceAll(replacement);
            json = DATE_OBJECT_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        private static String scrubTime(String json, String replacement) {
            json = RFC_TIME_PATTERN.matcher(json).replaceAll(replacement);
            json = TIME_OBJECT_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        private static String scrubTimestamp(String json, String replacement) {
            json = $DateScrubber.DATE_PATTERN.matcher(json).replaceAll(replacement);
            json = RFC_TIMESTAMP_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        public static String scrub(String json) {
            json = scrubVectorOrBinary(json, "\"vector_or_binary\"");
            json = scrubDate(json, "\"date_or_time\"");
            json = scrubTime(json, "\"date_or_time\"");
            json = scrubTimestamp(json, "\"date_or_time\"");
            return json;
        }
    }
}
