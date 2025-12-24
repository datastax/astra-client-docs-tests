package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import lombok.val;

import java.util.Map;
import java.util.function.Function;

import static com.dtsx.docs.lib.Constants.*;

public class SourceCodeReplacer {
    private static final Map<String, Function<VerifierConfig, String>> PLACEHOLDERS = Map.of(
        "KEYSPACE_NAME", _ -> TEST_KEYSPACE_NAME,
        "COLLECTION_NAME", _ -> TEST_COLLECTION_NAME,
        "TABLE_NAME", _ -> TEST_TABLE_NAME,
        "APPLICATION_TOKEN", VerifierConfig::token,
        "ASTRA_API_ENDPOINT", VerifierConfig::apiEndpoint
    );

    public static String replacePlaceholders(String src, VerifierConfig cfg) {
        var result = src;
        for (val entry : PLACEHOLDERS.entrySet()) {
            result = result.replace("**" + entry.getKey() + "**", entry.getValue().apply(cfg));
        }
        return result;
    }
}
