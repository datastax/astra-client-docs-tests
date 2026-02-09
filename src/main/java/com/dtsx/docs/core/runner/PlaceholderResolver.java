package com.dtsx.docs.core.runner;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import lombok.val;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Map<String, BiFunction<BaseScriptRunnerCtx, Placeholders, Optional<String>>> PLACEHOLDERS = Map.ofEntries(
        Map.entry("APPLICATION_TOKEN", (ctx, _) -> Optional.of(ctx.connectionInfo().token())),
        Map.entry("API_ENDPOINT", (ctx, _) -> Optional.of(ctx.connectionInfo().endpoint())),
        Map.entry("USERNAME", (ctx, _) -> ctx.connectionInfo().username()),
        Map.entry("PASSWORD", (ctx, _) -> ctx.connectionInfo().password()),
        Map.entry("KEYSPACE_NAME", (_, phs) -> Optional.of(phs.keyspaceName())),
        Map.entry("TABLE_NAME", (_, phs) -> phs.tableName()),
        Map.entry("COLLECTION_NAME", (_, phs) -> phs.collectionName()),
        Map.entry("DATABASE_NAME", (_, _) -> Optional.of("whatever_db_name")), // can be actually implemented later if needed
        Map.entry("DATABASE_ID", (_, _) -> Optional.of("whatever_db_id")), // can be actually implemented later if needed
        Map.entry("OLD_COLLECTION_NAME", (_, _) -> Optional.of("whatever_old_name")), // can be actually implemented later if needed
        Map.entry("NEW_COLLECTION_NAME", (_, _) -> Optional.of("whatever_new_name")) // can be actually implemented later if needed
    );

    private static final Pattern PLACEHOLDER = Pattern.compile("\\*\\*(\\w+)\\*\\*");

    private static final Map<String, String> VECTORS = Map.of(
        "0.08, -0.62, 0.39", randomVectorArray("vec1"),
        "0.08f, -0.62f, 0.39f", randomVectorArray("vec1"),
        "0.12, 0.53, 0.32", randomVectorArray("vec2"),
        "0.12f, 0.53f, 0.32f", randomVectorArray("vec2"),
        "PaPXCr8euFI+x64U", randomVectorBinary("bin1"),
        "PfXCjz8HrhQ+o9cK", randomVectorBinary("bin2")
    );

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

        VECTORS.forEach((vector, replacement) -> {
            val index = out.indexOf(vector);

            if (index >= 0) {
                out.replace(index, index + vector.length(), replacement);
            }
        });

        return out.toString();
    }

    public static Map<String, String> mkEnvVars(BaseScriptRunnerCtx ctx, Placeholders placeholders) {
        val envVars = new HashMap<String, String>();

        for (val entry : PLACEHOLDERS.entrySet()) {
            entry.getValue().apply(ctx, placeholders).ifPresent(v -> envVars.put(entry.getKey(), v));
        }

        return envVars;
    }

    private static String randomVectorArray(String seed) {
        val random = new Random(seed.hashCode());
        val sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < 1024; i++) {
            sb.append(random.nextFloat());
            if (i < 1023) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private static String randomVectorBinary(String seed) {
        val random = new Random(seed.hashCode());
        val bytes = new byte[1024 * Float.BYTES];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
