package com.dtsx.docs.runner.drivers;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.drivers.impls.BashDriver;
import com.dtsx.docs.runner.drivers.impls.JavaDriver;
import com.dtsx.docs.runner.drivers.impls.PythonDriver;
import com.dtsx.docs.runner.drivers.impls.TypeScriptDriver;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public abstract class ClientDriver {
    protected final String artifact;

    public abstract ClientLanguage language();

    public abstract List<Function<VerifierCtx, ExternalProgram>> requiredPrograms();

    public abstract Path setupExecutionEnvironment(VerifierCtx ctx, ExecutionEnvironment execEnv);

    public abstract String preprocessScript(VerifierCtx ignoredCtx, String content);

    public abstract RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv);
}
