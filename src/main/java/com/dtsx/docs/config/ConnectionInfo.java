package com.dtsx.docs.config;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.docs.core.runner.RunException;
import lombok.Getter;
import lombok.val;

import java.util.Base64;
import java.util.Optional;

/// Allows for abstraction over Astra vs HCD connections.
@Getter
public class ConnectionInfo {
    private final String token;
    private final String endpoint;
    private final DataAPIDestination destination;

    private final Optional<String> username;
    private final Optional<String> password;

    public ConnectionInfo(String token, String endpoint) {
        this.token = token;
        this.endpoint = endpoint;

        this.destination =
            (endpoint.contains("astra.datastax.com"))
                ? DataAPIDestination.ASTRA :
            (endpoint.contains("astra-dev.datastax.com"))
                ? DataAPIDestination.ASTRA_DEV :
            (endpoint.contains("astra-test.datastax.com"))
                ? DataAPIDestination.ASTRA_TEST
                : DataAPIDestination.HCD;

        if (token.startsWith("Cassandra:")) {
            val parts = token.split(":");

            if (parts.length != 3) {
                throw new RunException("Invalid Cassandra:... token format; expected 3 parts but got " + parts.length + " parts");
            }

            val decoder = Base64.getDecoder();

            this.username = Optional.of(new String(decoder.decode(parts[1])));
            this.password = Optional.of(new String(decoder.decode(parts[2])));
        } else {
            this.username = Optional.empty();
            this.password = Optional.empty();
        }
    }
}
