package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;

import static com.dtsx.docs.lib.Constants.*;

public class SourceCodeReplacer {
    public static final String TEST_KEYSPACE_PLACEHOLDER = "**KEYSPACE_NAME**";
    public static final String TEST_COLLECTION_PLACEHOLDER = "**COLLECTION_NAME**";
    public static final String TEST_TABLE_PLACEHOLDER = "**TABLE_NAME**";
    public static final String ASTRA_TOKEN_PLACEHOLDER = "**APPLICATION_TOKEN**";
    public static final String API_ENDPOINT_PLACEHOLDER = "**ASTRA_API_ENDPOINT**";

    public static String replacePlaceholders(String src, VerifierConfig cfg) {
        return src
            .replace(TEST_KEYSPACE_PLACEHOLDER, TEST_KEYSPACE_NAME)
            .replace(TEST_COLLECTION_PLACEHOLDER, TEST_COLLECTION_NAME)
            .replace(TEST_TABLE_PLACEHOLDER, TEST_TABLE_NAME)
            .replace(ASTRA_TOKEN_PLACEHOLDER, cfg.token())
            .replace(API_ENDPOINT_PLACEHOLDER, cfg.apiEndpoint());

    }
}
