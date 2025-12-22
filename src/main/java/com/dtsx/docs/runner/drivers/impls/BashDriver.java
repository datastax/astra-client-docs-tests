package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class BashDriver implements ClientDriver {
    @Override
    public ClientLanguage language() {
        return ClientLanguage.BASH;
    }

    @Override
    public List<Function<VerifierConfig, ExternalProgram>> requiredPrograms() {
        return List.of(
            ExternalPrograms::bash
        );
    }

    @Override
    public Path testFileCopyDestination(Path tempEnv) {
        return tempEnv.resolve("main.sh");
    }

    @Override
    public RunResult execute(VerifierConfig cfg, Path script) {
        return ExternalPrograms.bash(cfg).run(script.toAbsolutePath().toString());
    }
}
