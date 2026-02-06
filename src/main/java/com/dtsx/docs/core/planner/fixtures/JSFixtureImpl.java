package com.dtsx.docs.core.planner.fixtures;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.runner.PlaceholderResolver;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.RunException;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.ExternalPrograms.StderrLine;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public final class JSFixtureImpl extends JSFixture {
    private final TestCtx ctx;
    private final Path path;
    private final boolean dryRun;

    @Override
    public String fixtureName() {
        return path.getFileName().toString();
    }

    @Override
    public Placeholders meta(ExternalProgram tsx) {
        val maybeOutput = tryCallJsFunction(tsx, Placeholders.EMPTY, "Meta");

        // don't actually have fixture metadata here, but we can just pass the empty value
        // since it shouldn't be used outside of actual fixture functions
        if (maybeOutput.isEmpty()) {
            return Placeholders.EMPTY;
        }

        val output = maybeOutput.get().stdout();

        try {
            return JacksonUtils.parseJson(output, Placeholders.class);
        } catch (Exception e) {
            throw new RunException("Failed to parse fixture metadata JSON from " + path + ":\n" + output, e);
        }
    }

    @Override
    protected void setup(ExternalProgram tsx, Placeholders placeholders) {
        if (!dryRun) {
            tryCallJsFunction(tsx, placeholders, "Setup");
        }
    }

    @Override
    protected void beforeEach(ExternalProgram tsx, Placeholders placeholders) {
        if (!dryRun) {
            tryCallJsFunction(tsx, placeholders, "BeforeEach"); // TODO cache if any of the methods aren't present (if possible?)
        }
    }

    @Override
    protected void afterEach(ExternalProgram tsx, Placeholders placeholders) {
        if (!dryRun) {
            tryCallJsFunction(tsx, placeholders, "AfterEach");
        }
    }

    @Override
    protected void teardown(ExternalProgram tsx, Placeholders placeholders) {
        if (!dryRun) {
            tryCallJsFunction(tsx, placeholders, "Teardown");
        }
    }

    private final Set<String> nonexistentFunctions = new HashSet<>();

    @SneakyThrows
    private Optional<RunResult> tryCallJsFunction(ExternalProgram tsx, Placeholders placeholders, String function) {
        if (nonexistentFunctions.contains(function)) {
            return Optional.empty();
        }

        if (!Files.readString(path).contains(function)) {
            nonexistentFunctions.add(function);
            return Optional.empty();
        }

        val displayPath = ctx.examplesFolder().relativize(path);

        val envVars = new HashMap<>(PlaceholderResolver.mkEnvVars(ctx, placeholders));

        val code = """
          import * as m from '%s';
        
          const fn = m.%s;
        
          if (fn) {
            console.log(fn());
          } else {
            console.error("function_not_found");
          }
        """.formatted(path.toAbsolutePath(), function);

        // Calls the function if it exists
        val res = CliLogger.loading("Calling @!%s!@ in @!%s!@".formatted(function, displayPath), (_) -> {
            return tsx.run(null, envVars, "-e", code);
        });

        if (res.exitCode() != 0) {
            throw new RunException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }

        if (res.stdout().contains("function_not_found")) {
            nonexistentFunctions.add(function);
            return Optional.empty();
        }

        for (val line : res.outputLines()) {
            if (line instanceof StderrLine(String unwrap)) {
                CliLogger.debug("[%s/%s] %s".formatted(displayPath, function, unwrap));
            }
        }

        return Optional.of(res);
    }
}
