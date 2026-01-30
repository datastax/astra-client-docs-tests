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
    public List<Function<BaseScriptRunnerCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::python);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val python = ExternalPrograms.python(ctx);

        val mkVenv = python.run(execEnv.envDir(), "-m", "venv", ".venv");
        if (mkVenv.notOk()) {
            throw new RuntimeException("Failed to create Python virtual environment:\n" + mkVenv.output());
        }

        replaceArtifactPlaceholder(execEnv, "requirements.txt");

        val install = ExternalPrograms.custom(ctx).run(execEnv.envDir(), ".venv/bin/pip", "install", "-U", "-r", "requirements.txt", artifact());
        if (install.notOk()) {
            throw new RuntimeException("Failed to install Python dependencies:\n" + install.output());
        }

        return execEnv.envDir().resolve("example.py");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content) {
        // Add JSON encoder for Data API types at the beginning of the script
        String jsonEncoderPrelude = """
            import os
            import json as _json_module
            from astrapy.data_types import DataAPITimestamp as _DataAPITimestamp
            
            class _DataAPIEncoder(_json_module.JSONEncoder):
                def default(self, o):
                    if isinstance(o, _DataAPITimestamp):
                        return o.to_string()
                    return super().default(o)
            
            def _contains_data_api_types(obj):
                '''Recursively check if object contains Data API types'''
                if isinstance(obj, _DataAPITimestamp):
                    return True
                elif isinstance(obj, dict):
                    return any(_contains_data_api_types(v) for v in obj.values())
                elif isinstance(obj, (list, tuple)):
                    return any(_contains_data_api_types(item) for item in obj)
                return False
            
            # Override print to automatically JSON-encode Data API types
            _original_print = print
            def print(*args, **kwargs):
                json_args = []
                for arg in args:
                    if _contains_data_api_types(arg):
                        json_args.append(_json_module.dumps(arg, cls=_DataAPIEncoder))
                    else:
                        json_args.append(arg)
                _original_print(*json_args, **kwargs)
            
            """;

        return jsonEncoderPrelude + content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        // The preprocessScript method now handles JSON encoding of Data API types,
        // so we only need to handle Python boolean literals here
        String processed = content.replace(" True,", " true,").replace(" False,", " false,");
        return JacksonUtils.parseJsonRoots(processed, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom(ctx).run(execEnv.envDir(), "./.venv/bin/python", "-m", "mypy", "example.py");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom(ctx).run(execEnv.envDir(), envVars, ".venv/bin/python", execEnv.scriptPath());
    }
}
