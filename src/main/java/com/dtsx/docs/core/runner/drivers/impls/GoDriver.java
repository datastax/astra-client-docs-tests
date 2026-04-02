package com.dtsx.docs.core.runner.drivers.impls;

import com.dtsx.docs.config.ctx.BaseCtx;
import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.docs.core.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.docs.core.runner.RunException;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;
import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GoDriver extends ClientDriver {
    public GoDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.GO;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::go);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val go = ExternalPrograms.go(ctx);

        val getClient = go.run(execEnv.envDir(), "get", artifact());

        if (getClient.notOk()) {
            throw new RunException("Failed to get Go client dependency:\n" + getClient.output());
        }

        return execEnv.envDir().resolve("example.go");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = """
            package main
            
            import (
                "encoding/json"
                "fmt"
                "os"
            )
            
            var originalPrintln = fmt.Println
            
            func init() {
                fmt.Println = func(a ...any) (n int, err error) {
                    for _, v := range a {
                        jsonBytes, err := json.Marshal(v)
                        if err != nil {
                            return originalPrintln(v)
                        }
                        originalPrintln(string(jsonBytes))
                    }
                    return len(a), nil
                }
            }
            """ + content.replaceFirst("package main\\s*", "");
        }

        return content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.go(ctx).run(execEnv.envDir(), "build", execEnv.scriptPath());
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.go(ctx).run(execEnv.envDir(), envVars, "run", execEnv.scriptPath());
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val result = ExternalPrograms.go(ctx).run(execEnv.envDir(), "list", "-m", "-json", "all");

        if (result.notOk()) {
            throw new RunException("Failed to extract Go client version: " + result.output());
        }

        try {
            val it = JacksonUtils.parseJsonLines(result.stdout(), JsonNode.class);

            for (val node : it) {
                val path = node.path("Path").asString();

                if (path.endsWith("astra-db-go") && node.has("Version")) {
                    val version = node.path("Version").asString();

                    if (!version.isEmpty()) {
                        return Optional.of(version);
                    }
                }
            }

        } catch (Exception e) {
            return Optional.of("unknown");
        }

        return Optional.of("unknown");
    }
}
