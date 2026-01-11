package com.dtsx.docs.runner;

import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.config.VerifierCtx;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Map<String, BiFunction<VerifierCtx, FixtureMetadata, Optional<String>>> PLACEHOLDERS = Map.of(
        "APPLICATION_TOKEN", (ctx, _) -> Optional.of(ctx.connectionInfo().token()),
        "API_ENDPOINT", (ctx, _) -> Optional.of(ctx.connectionInfo().endpoint()),
        "KEYSPACE_NAME", (_, md) -> Optional.of( md.keyspaceName()),
        "TABLE_NAME", (_, md) -> md.tableName(),
        "COLLECTION_NAME", (_, md) -> md.collectionName()
    );

    private static final Pattern PLACEHOLDER = Pattern.compile("\\*\\*(\\w+)\\*\\*");

    public static String replacePlaceholders(VerifierCtx ctx, FixtureMetadata md, String src) {
        val m = PLACEHOLDER.matcher(src);
        val out = new StringBuilder(src.length());

        while (m.find()) {
            val key = m.group(1);
            val value = PLACEHOLDERS.get(key).apply(ctx, md).orElseThrow(() -> new TestRunException("Missing value for placeholder: **" + key + "**"));

            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }

        m.appendTail(out);
        return out.toString();
    }

    public static Map<String, String> mkEnvVars(VerifierCtx ctx, FixtureMetadata md) {
        val envVars = new HashMap<String, String>();

        for (val entry : PLACEHOLDERS.entrySet()) {
            entry.getValue().apply(ctx, md).ifPresent(v -> envVars.put(entry.getKey(), v));
        }

        return envVars;
    }
}
