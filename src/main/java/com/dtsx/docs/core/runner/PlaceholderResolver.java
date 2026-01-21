package com.dtsx.docs.core.runner;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Map<String, BiFunction<BaseScriptRunnerCtx, Placeholders, Optional<String>>> PLACEHOLDERS = Map.of(
        "APPLICATION_TOKEN", (ctx, _) -> Optional.of(ctx.connectionInfo().token()),
        "API_ENDPOINT", (ctx, _) -> Optional.of(ctx.connectionInfo().endpoint()),
        "USERNAME", (ctx, _) -> ctx.connectionInfo().username(),
        "PASSWORD", (ctx, _) -> ctx.connectionInfo().password(),
        "KEYSPACE_NAME", (_, phs) -> Optional.of(phs.keyspaceName()),
        "TABLE_NAME", (_, phs) -> phs.tableName(),
        "COLLECTION_NAME", (_, phs) -> phs.collectionName(),
        "DATABASE_NAME", (_, _) -> Optional.of("whatever_db_name") // can be actually implemented later if needed
    );

    private static final Pattern PLACEHOLDER = Pattern.compile("\\*\\*(\\w+)\\*\\*");

    public static String replacePlaceholders(BaseScriptRunnerCtx ctx, Placeholders placeholders, String src) {
        val m = PLACEHOLDER.matcher(src);
        val out = new StringBuilder(src.length());

        while (m.find()) {
            val key = m.group(1);

            if (!PLACEHOLDERS.containsKey(key)) {
                throw new RunException("Unknown placeholder: **" + key + "**");
            }

            val value = PLACEHOLDERS.get(key).apply(ctx, placeholders).orElseThrow(() -> new RunException("Missing value for placeholder: **" + key + "**"));

            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }

        m.appendTail(out);
        return out.toString();
    }

    public static Map<String, String> mkEnvVars(BaseScriptRunnerCtx ctx, Placeholders placeholders) {
        val envVars = new HashMap<String, String>();

        for (val entry : PLACEHOLDERS.entrySet()) {
            entry.getValue().apply(ctx, placeholders).ifPresent(v -> envVars.put(entry.getKey(), v));
        }

        return envVars;
    }
}
