package com.dtsx.docs.config;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.docs.core.runner.RunException;
import lombok.Getter;
import lombok.val;
import org.intellij.lang.annotations.MagicConstant;

import java.util.Base64;
import java.util.Optional;

/// Allows for abstraction over Astra vs HCD connections.
@Getter
public class ConnectionInfo {
    private final String token;
    private final String endpoint;
    private final DataAPIDestination destination;
    private final @MagicConstant(stringValues = { "prod", "dev", "test" }) Optional<String> astraEnv;

    private final Optional<String> username;
    private final Optional<String> password;

    public ConnectionInfo(String token, String endpoint) {
        this.token = token;
        this.endpoint = endpoint;

        if (endpoint.contains("astra.datastax.com")) {
            this.destination = DataAPIDestination.ASTRA;
            this.astraEnv = Optional.of("prod");
        } else if (endpoint.contains("astra-dev.datastax.com")) {
            this.destination = DataAPIDestination.ASTRA_DEV;
            this.astraEnv = Optional.of("dev");
        } else if (endpoint.contains("astra-test.datastax.com")) {
            this.destination = DataAPIDestination.ASTRA_TEST;
            this.astraEnv = Optional.of("test");
        } else {
            this.destination = DataAPIDestination.HCD;
            this.astraEnv = Optional.empty();
        }

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
