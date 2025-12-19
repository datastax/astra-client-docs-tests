package com.dtsx.docs.drivers;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalRunners.RunResult;

import java.nio.file.Path;
import java.util.Optional;

public abstract class ClientDriver {
    public abstract ClientLanguage language();

    public abstract Path executionLocation(Path tempEnv);

    public abstract RunResult execute(VerifierConfig cfg, Path script);

    public abstract RunResult beforeAll(VerifierConfig cfg, Path tempEnv);

    public static Optional<ClientDriver> mkIfAvailable(String driver) {
        return switch (driver.toLowerCase()) {
            case "typescript" -> Optional.of(new TypeScriptDriver());
            default -> Optional.empty();
        };
    }
}
