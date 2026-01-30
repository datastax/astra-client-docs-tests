package com.dtsx.docs.core.runner.drivers.impls;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.docs.core.planner.meta.snapshot.sources.OutputJsonifySourceMeta;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.JacksonUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.dtsx.docs.lib.JacksonUtils.runJq;

public class BashDriver extends ClientDriver {
    public BashDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.BASH;
    }

    @Override
    public List<Function<BaseScriptRunnerCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::bash);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return execEnv.envDir().resolve("example.sh");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content) {
        return "#!/bin/bash\n\nset -euo pipefail\n\n" + content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonRoots(runJq(ctx, content, meta.jqBash()), Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.bash(ctx).run(execEnv.envDir(), "-n", execEnv.scriptPath());
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.bash(ctx).run(execEnv.envDir(), envVars, execEnv.scriptPath());
    }
}
