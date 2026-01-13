package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.TestRunException;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
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
    public List<Function<VerifierCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::java);
    }

    @Override
    public Path setupExecutionEnvironment(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        replaceArtifactPlaceholder(execEnv, "build.gradle");

        val build = ExternalPrograms.custom(ctx).run(execEnv.envDir(), "./gradlew", "build");
        if (build.notOk()) {
            throw new TestRunException("Failed to build Java client:\n" + build.output());
        }

        return execEnv.envDir().resolve("src/main/java/Example.java");
    }

    @Override
    public String preprocessScript(VerifierCtx ignoredCtx, String content) {
        return Pattern.compile("^public\\s+class\\s+\\w+", Pattern.MULTILINE)
            .matcher(content)
            .replaceFirst("public class Example");
    }

    @Override
    public RunResult compileScript(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom(ctx).run(execEnv.envDir(), "./gradlew", "build");
    }

    @Override
    public RunResult executeScript(VerifierCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom(ctx).run(execEnv.envDir(), envVars, "./gradlew", "run", "--quiet");
    }
}
