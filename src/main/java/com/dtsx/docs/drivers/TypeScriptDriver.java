package com.dtsx.docs.drivers;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalRunners;
import com.dtsx.docs.lib.ExternalRunners.RunResult;

import java.nio.file.Path;

public class TypeScriptDriver extends ClientDriver {
    @Override
    public ClientLanguage language() {
        return ClientLanguage.TYPESCRIPT;
    }

    @Override
    public Path executionLocation(Path tempEnv) {
        return tempEnv.resolve("main.ts");
    }

    @Override
    public RunResult beforeAll(VerifierConfig cfg, Path tempEnv) {
        return ExternalRunners.npm(cfg).run(tempEnv, "install");
    }

    @Override
    public RunResult execute(VerifierConfig cfg, Path script) {
        return ExternalRunners.tsx(cfg).run(script.toAbsolutePath().toString());
    }
}
