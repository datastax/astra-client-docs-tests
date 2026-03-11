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
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
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
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = """
            const _log = console.log;
            
            import { stringify } from "json-bigint";
            
            (console.log as any) = (json: any) => {
              if (typeof json !== "object" || json === null) {
                _log(stringify(json));
                return;
              }
            
              for (const [key, value] of Object.entries(json)) {
                if (value instanceof Map) {
                  json[key] = Object.fromEntries(value);
                }
                if (value instanceof Set) {
                  json[key] = Array.from(value);
                }
              }
              _log(stringify(json));
            };
            """ + content;
        }

        return content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.npm(ctx).run(execEnv.envDir(), "run", "typecheck");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.tsx(ctx).run(execEnv.envDir(), envVars, execEnv.scriptPath());
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        try {
            val packageLockPath = execEnv.envDir().resolve("package-lock.json");
            val json = JacksonUtils.parseJson(Files.readString(packageLockPath), ObjectNode.class);
            val packages = json.get("packages");
            if (packages != null && packages.has("node_modules/@datastax/astra-db-ts")) {
                val version = packages.get("node_modules/@datastax/astra-db-ts").get("version").asString();
                return Optional.of("v" + version);
            }
            throw new RunException("Could not find @datastax/astra-db-ts in package-lock.json");
        } catch (Exception e) {
            throw new RunException("Failed to extract TypeScript client version from package-lock.json", e);
        }
    }
}
