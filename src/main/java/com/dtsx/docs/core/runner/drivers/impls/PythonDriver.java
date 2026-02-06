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
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
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
            
            # Helper function to convert objects to serializable format
            def _convert_to_serializable(obj):
                # Try to_dict() method first
                if hasattr(obj, 'to_dict') and callable(getattr(obj, 'to_dict')):
                    return _convert_to_serializable(obj.to_dict())
                # Try as_dict() method
                elif hasattr(obj, 'as_dict') and callable(getattr(obj, 'as_dict')):
                    return _convert_to_serializable(obj.as_dict())
                # Handle lists
                elif isinstance(obj, list):
                    return [_convert_to_serializable(item) for item in obj]
                # Handle tuples
                elif isinstance(obj, tuple):
                    return tuple(_convert_to_serializable(item) for item in obj)
                # Handle dicts
                elif isinstance(obj, dict):
                    return {k: _convert_to_serializable(v) for k, v in obj.items()}
                # For custom objects with __dict__, convert to dict
                elif hasattr(obj, '__dict__') and not isinstance(obj, type) and not callable(obj):
                    # Check if it's a custom class instance (not a built-in type)
                    if type(obj).__module__ not in ('builtins', '__builtin__'):
                        obj_dict = {}
                        for key, value in obj.__dict__.items():
                            if not key.startswith('_'):
                                obj_dict[key] = _convert_to_serializable(value)
                        return obj_dict
                return obj
            
            # Helper to ensure options field exists in definitions
            def _ensure_options_field(obj):
                if isinstance(obj, list):
                    return [_ensure_options_field(item) for item in obj]
                elif isinstance(obj, dict):
                    result = {}
                    for k, v in obj.items():
                        result[k] = _ensure_options_field(v)
                    # If this is a definition dict without options, add empty options
                    if 'definition' in result and isinstance(result['definition'], dict):
                        if 'options' not in result['definition']:
                            result['definition']['options'] = {}
                    return result
                return obj
            
            # Override print to automatically preprocess Data API types
            _original_print = print
            def print(*args, **kwargs):
                json_args = []
                for arg in args:
                    # First convert objects to serializable format
                    converted_arg = _convert_to_serializable(arg)
                    # Then preprocess for Data API types
                    preprocessed_arg = preprocess_table_payload(converted_arg, _serdes_options, None)
                    # Ensure options field exists in all definitions
                    final_arg = _ensure_options_field(preprocessed_arg)
                    json_args.append(_json_module.dumps(final_arg, separators=(',', ':')))
                _original_print(*json_args, **kwargs)
            """ + content;
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
