package com.dtsx.docs.core.runner;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import lombok.val;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Map<String, BiFunction<BaseScriptRunnerCtx, Placeholders, Optional<String>>> PLACEHOLDERS = new HashMap<>() {{
        put("APPLICATION_TOKEN", (ctx, _) -> Optional.of(ctx.connectionInfo().token()));
        put("API_ENDPOINT", (ctx, _) -> Optional.of(ctx.connectionInfo().endpoint()));
        put("USERNAME", (ctx, _) -> ctx.connectionInfo().username());
        put("PASSWORD", (ctx, _) -> ctx.connectionInfo().password());
        put("KEYSPACE_NAME", (_, phs) -> Optional.of(phs.keyspaceName()));
        put("TABLE_NAME", (_, phs) -> phs.tableName());
        put("COLLECTION_NAME", (_, phs) -> phs.collectionName());
        put("UDT_NAME", (_, _) -> Optional.of("placeholder_udt_name")); // can be actually implemented later if needed
        put("INDEX_NAME", (_, _) -> Optional.of("placeholder_index_name"));
        put("DATABASE_NAME", (_, _) -> Optional.of("whatever_db_name"));
        put("DATABASE_ID", (_, _) -> Optional.of("whatever_db_id"));
        put("OLD_COLLECTION_NAME", (_, _) -> Optional.of("whatever_old_name"));
        put("NEW_COLLECTION_NAME", (_, _) -> Optional.of("whatever_new_name"));
    }};

    private static final Pattern PLACEHOLDER = Pattern.compile("\\*\\*(\\w+)\\*\\*");

    private static final Map<String, String> VECTORS = Map.of(
        "0.08, -0.62, 0.39", randomVectorArray("vec1", false),
        "0.08f, -0.62f, 0.39f", randomVectorArray("vec1", true),
        "0.12, 0.53, 0.32", randomVectorArray("vec2", false),
        "0.12f, 0.53f, 0.32f", randomVectorArray("vec2", true),
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

    private static String randomVectorArray(String seed, boolean appendF) {
        val random = new Random(seed.hashCode());
        val sb = new StringBuilder();

        for (int i = 0; i < 1024; i++) {
            sb.append('0');
            sb.append('.');
            sb.append(random.nextInt(1, 10));
            if (appendF) {
                sb.append('f');
            }
            if (i < 1023) {
                sb.append(',');
            }
        }

        return sb.toString();
    }

    private static String randomVectorBinary(String seed) {
        val random = new Random(seed.hashCode());
        val bytes = new byte[1024 * Float.BYTES];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) random.nextInt(40, 60); // random bounds that don't seem to cause issues
        }

        return Base64.getEncoder().encodeToString(bytes);
    }
}
