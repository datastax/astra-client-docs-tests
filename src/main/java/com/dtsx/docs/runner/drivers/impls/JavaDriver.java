package com.dtsx.docs.runner.drivers.impls;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

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
       try {
           val buildGradle = Files.readString(execEnv.path().resolve("build.gradle"));
           val updatedBuildGradle = buildGradle.replace("${CLIENT_ARTIFACT}", artifact);
           Files.writeString(execEnv.path().resolve("build.gradle"), updatedBuildGradle);
       } catch (Exception e) {
           throw new RuntimeException("Failed to update build.gradle with client version", e);
       }

        ExternalPrograms.custom(ctx).run(execEnv.path(), "./gradlew", "build");

        return execEnv.path().resolve("src/main/java/Example.java");
    }

    @Override
    public String preprocessScript(VerifierCtx ignoredCtx, String content) {
        return content;
    }

    @Override
    public RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom(ctx).run(execEnv.path(), "./gradlew", "run", "--quiet");
    }
}
