package com.dtsx.docs.config;

import com.datastax.astra.client.DataAPIDestination;
import lombok.val;

public record ConnectionInfo(String token, String endpoint, DataAPIDestination destination) {
    public static ConnectionInfo parse(String token, String endpoint) {
        val dest =
            (endpoint.contains("astra.datastax.com"))
                ? DataAPIDestination.ASTRA :
            (endpoint.contains("astra-dev.datastax.com"))
                ? DataAPIDestination.ASTRA_DEV :
            (endpoint.contains("astra-test.datastax.com"))
                ? DataAPIDestination.ASTRA_TEST
                : DataAPIDestination.HCD;

        return new ConnectionInfo(token, endpoint, dest);
    }
}
