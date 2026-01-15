package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.RunException;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TypeScriptDriver extends ClientDriver {
    public TypeScriptDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.TYPESCRIPT;
    }

    @Override
    public List<Function<BaseScriptRunnerCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::npm, ExternalPrograms::tsx);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val res = ExternalPrograms.npm(ctx).run(execEnv.envDir(), "install", artifact());

        if (res.exitCode() != 0) {
            throw new RunException("Failed to setup TypeScript environment: " + res.output());
        }

        return execEnv.envDir().resolve("example.ts");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content) {
        return content;
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.npm(ctx).run(execEnv.envDir(), "run", "typecheck");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.tsx(ctx).run(execEnv.envDir(), envVars, execEnv.scriptPath());
    }
}
