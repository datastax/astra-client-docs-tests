package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class TypeScriptDriver implements ClientDriver {
    @Override
    public ClientLanguage language() {
        return ClientLanguage.TYPESCRIPT;
    }

    @Override
    public List<Function<VerifierConfig, ExternalProgram>> requiredPrograms() {
        return List.of(
            ExternalPrograms::npm,
            ExternalPrograms::tsx
        );
    }

    @Override
    public Path testFileCopyDestination(Path tempEnv) {
        return tempEnv.resolve("main.ts");
    }

    @Override
    public void setup(VerifierConfig cfg, Path tempEnv) {
        val res = ExternalPrograms.npm(cfg).run(tempEnv, "install");

        if (res.exitCode() != 0) {
            throw new IllegalStateException("Failed to setup TypeScript environment: " + res.output());
        }
    }

    @Override
    public RunResult execute(VerifierConfig cfg, Path script) {
        return ExternalPrograms.tsx(cfg).run(script.toAbsolutePath().toString());
    }
}
