package com.dtsx.docs.core.runner.drivers.impls;

import com.dtsx.docs.config.ctx.BaseCtx;
import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.docs.core.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
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
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
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

        val install = ExternalPrograms.custom().run(execEnv.envDir(), ".venv/bin/pip", "install", "-U", "-r", "requirements.txt", artifact());
        if (install.notOk()) {
            throw new RuntimeException("Failed to install Python dependencies:\n" + install.output());
        }

        return execEnv.envDir().resolve("example.py");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ctx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = """
            import json as _json_module
            from astrapy.data.utils.table_converters import preprocess_table_payload, FullSerdesOptions
            
            # Create serdes options for converting Data API types
            _serdes_options = FullSerdesOptions(
                binary_encode_vectors=False,
                custom_datatypes_in_reading=True,
                unroll_iterables_to_lists=True,
                use_decimals_in_collections=False,
                encode_maps_as_lists_in_tables=False,
                accept_naive_datetimes=False,
                datetime_tzinfo=None,
                serializer_by_class={},
                deserializer_by_udt={}
            )
            
            # Override print to automatically preprocess Data API types
            _original_print = print
            def print(*args, **kwargs):
                json_args = []
                for arg in args:
                    json_args.append(_json_module.dumps(preprocess_table_payload(arg, _serdes_options, None), separators=(',', ':')))
                _original_print(*json_args, **kwargs)
            """ + content;
        }

        if (ctx.connectionInfo().astraEnv().isPresent()) {
            content = content.replace("DataAPIClient(", "DataAPIClient(environment=parse_api_endpoint(\"" + ctx.connectionInfo().endpoint() + "\").environment,");
        }

        return content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonRoots(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom().run(execEnv.envDir(), "./.venv/bin/python", "-m", "mypy", "example.py");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom().run(execEnv.envDir(), envVars, ".venv/bin/python", execEnv.scriptPath());
    }
}
