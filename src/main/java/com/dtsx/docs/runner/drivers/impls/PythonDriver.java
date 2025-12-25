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

public class PythonDriver implements ClientDriver {
    @Override
    public ClientLanguage language() {
        return ClientLanguage.PYTHON;
    }

    @Override
    public List<Function<VerifierCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::python);
    }

    @Override
    public Path setupExecutionEnvironment(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return execEnv.path().resolve("main.py");
    }

    @Override
    public String preprocessScript(VerifierCtx ignoredCtx, String content) {
        return content;
    }

    @Override
    public RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return null;
    }
}
