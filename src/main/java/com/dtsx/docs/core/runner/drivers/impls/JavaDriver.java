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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class JavaDriver extends ClientDriver {
    public JavaDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.JAVA;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::java);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        replaceArtifactPlaceholder(execEnv, "build.gradle");

        val build = ExternalPrograms.custom().run(execEnv.envDir(), "./gradlew", "build");
        if (build.notOk()) {
            throw new RunException("Failed to build Java client:\n" + build.output());
        }

        return execEnv.envDir().resolve("src/main/java/Example.java");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = """
               import com.fasterxml.jackson.databind.json.JsonMapper;
               import com.dtsx.astra.sdk.utils.JsonUtils;
               import java.io.PrintStream;
            """ + content;

            val target = "public static void main(String[] args) {";
            val mainMethodIdx = content.indexOf(target);

            if (mainMethodIdx == -1) {
                throw new RunException("main method not found");
            }

            val insertPos = mainMethodIdx + target.length();

            content =
                content.substring(0, insertPos)+
                """
                System.setOut(new PrintStream(System.out) {
                    @Override
                    public void println(Object obj) {
                        try {
                            super.println(JsonUtils.getObjectMapper().findAndRegisterModules().writeValueAsString(obj));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                """
                + content.substring(insertPos);
        }

        return Pattern.compile("^public\\s+class\\s+\\w+", Pattern.MULTILINE)
            .matcher(content)
            .replaceFirst("public class Example");
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom().run(execEnv.envDir(), "./gradlew", "build");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom().run(execEnv.envDir(), envVars, "./gradlew", "run", "--quiet");
    }
}
