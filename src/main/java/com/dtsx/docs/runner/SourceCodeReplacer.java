package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import lombok.val;

import java.util.Map;
import java.util.function.Function;

import static com.dtsx.docs.lib.Constants.*;

public class SourceCodeReplacer {
    private static final Map<String, Function<VerifierCtx, String>> PLACEHOLDERS = Map.of(
        "KEYSPACE_NAME", _ -> TEST_KEYSPACE_NAME,
        "COLLECTION_NAME", _ -> TEST_COLLECTION_NAME,
        "TABLE_NAME", _ -> TEST_TABLE_NAME,
        "APPLICATION_TOKEN", VerifierCtx::token,
        "API_ENDPOINT", VerifierCtx::apiEndpoint
    );

    public static String replacePlaceholders(String src, VerifierCtx ctx) {
        var result = src;
        for (val entry : PLACEHOLDERS.entrySet()) {
            result = result.replace("**" + entry.getKey() + "**", entry.getValue().apply(ctx));
        }
        return result;
    }

    public static Map<String, String> mkEnvVars(VerifierCtx ctx) {
        val envVars = new java.util.HashMap<String, String>();
        for (val entry : PLACEHOLDERS.entrySet()) {
            envVars.put(entry.getKey(), entry.getValue().apply(ctx));
        }
        return envVars;
    }
}
