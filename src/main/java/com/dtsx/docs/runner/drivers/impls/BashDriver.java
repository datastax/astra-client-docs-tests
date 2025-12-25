package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
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
    public List<Function<VerifierCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::bash);
    }

    @Override
    public Path setupExecutionEnvironment(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return execEnv.path().resolve("main.sh");
    }

    @Override
    public String preprocessScript(VerifierCtx ignoredCtx, String content) {
        return "#!/bin/bash\n\nset -euo pipefail\n\n" + content;
    }

    @Override
    public RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.bash(ctx).run(execEnv.path(), execEnv.scriptPath().toString());
    }
}
