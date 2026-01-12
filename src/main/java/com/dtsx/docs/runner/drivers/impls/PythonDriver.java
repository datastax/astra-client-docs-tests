package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PythonDriver extends ClientDriver {
    public PythonDriver(String artifact) {
        super(artifact);
    }

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
        val python = ExternalPrograms.python(ctx);

        if (!python.run(execEnv.envDir(), "-m", "venv", ".venv").ok()) {
            throw new RuntimeException("Failed to create Python virtual environment");
        }

        replaceArtifactPlaceholder(execEnv, "requirements.txt");

        val cmd = ExternalPrograms.custom(ctx);

        if (!cmd.run(execEnv.envDir(), ".venv/bin/pip", "install", "-U", "-r", "requirements.txt", artifact()).ok()) {
            throw new RuntimeException("Failed to upgrade pip in Python virtual environment");
        }

        return execEnv.envDir().resolve("main.py");
    }

    @Override
    public String preprocessScript(VerifierCtx ignoredCtx, String content) {
        return "import os\n\n" + content;
    }

    @Override
    public RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom(ctx).run(execEnv.envDir(), envVars, ".venv/bin/python", execEnv.scriptPath());
    }
}
