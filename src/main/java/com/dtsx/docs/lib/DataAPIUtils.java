package com.dtsx.docs.lib;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.Collection;
import com.dtsx.docs.VerifierConfig;

import static com.dtsx.docs.lib.Constants.TEST_COLLECTION_NAME;

public class DataAPIUtils {
    public static Collection<?> getCollection(VerifierConfig cfg) {
        return new DataAPIClient(cfg.token()).getDatabase(cfg.apiEndpoint()).getCollection(TEST_COLLECTION_NAME);
    }
}
