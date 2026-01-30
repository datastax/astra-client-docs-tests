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
        // Use astrapy's built-in serialization to convert Data API types to EJSON format
        String jsonEncoderPrelude = """
            import os
            import json as _json_module
            from astrapy.data.utils.collection_converters import preprocess_collection_payload_value, FullSerdesOptions
            
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
            
            def _preprocess_for_json(obj):
                '''Recursively preprocess object to convert Data API types to JSON-serializable format'''
                if isinstance(obj, dict):
                    return {k: _preprocess_for_json(v) for k, v in obj.items()}
                elif isinstance(obj, (list, tuple)):
                    return [_preprocess_for_json(item) for item in obj]
                else:
                    # Use astrapy's built-in converter
                    processed = preprocess_collection_payload_value([], obj, _serdes_options)
                    # If it's still not JSON serializable (like DataAPIVector), convert to list
                    if hasattr(processed, '__iter__') and not isinstance(processed, (str, dict)):
                        try:
                            _json_module.dumps(processed)
                            return processed
                        except (TypeError, ValueError):
                            return list(processed)
                    return processed
            
            # Override print to automatically preprocess Data API types
            _original_print = print
            def print(*args, **kwargs):
                json_args = []
                for arg in args:
                    try:
                        # Try to serialize directly first
                        _json_module.dumps(arg)
                        json_args.append(arg)
                    except (TypeError, ValueError):
                        # If it fails, preprocess and serialize
                        preprocessed = _preprocess_for_json(arg)
                        json_args.append(_json_module.dumps(preprocessed))
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
