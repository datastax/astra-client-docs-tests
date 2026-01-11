package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CSharpDriver extends ClientDriver {
    public CSharpDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return null;
    }

    @Override
    public List<Function<VerifierCtx, ExternalProgram>> requiredPrograms() {
        return List.of();
    }

    @Override
    public Path setupExecutionEnvironment(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return null;
    }

    @Override
    public String preprocessScript(VerifierCtx ignoredCtx, String content) {
        return "";
    }

    @Override
    public RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return null;
    }
}
