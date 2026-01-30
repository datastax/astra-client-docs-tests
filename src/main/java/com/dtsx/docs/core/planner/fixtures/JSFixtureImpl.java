package com.dtsx.docs.core.planner.fixtures;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import com.dtsx.docs.core.runner.PlaceholderResolver;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.RunException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Path;
import java.util.HashMap;

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
        // don't actually have fixture metadata here, but we can just pass the empty value
        // since it shouldn't be used outside of actual fixture functions
        val output = tryCallJsFunction(tsx, Placeholders.EMPTY, "Meta").stdout().trim();

        if (output.equals("undefined")) {
            return Placeholders.EMPTY;
        }

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
    protected void reset(ExternalProgram tsx, Placeholders placeholders) {
        if (!dryRun) {
            tryCallJsFunction(tsx, placeholders, "Reset"); // TODO cache if any of the method aren't present (if possible?)
        }
    }

    @Override
    protected void teardown(ExternalProgram tsx, Placeholders placeholders) {
        if (!dryRun) {
            tryCallJsFunction(tsx, placeholders, "Teardown");
        }
    }

    @SneakyThrows
    private RunResult tryCallJsFunction(ExternalProgram tsx, Placeholders placeholders, String function) {
        val displayPath = ctx.examplesFolder().relativize(path);

        val envVars = new HashMap<>(PlaceholderResolver.mkEnvVars(ctx, placeholders));

        // Calls the function if it exists
        val res = CliLogger.loading("Calling @!%s!@ in @!%s!@".formatted(function, displayPath), (_) -> {
            return tsx.run(null, envVars, "-e", "import * as m from '" + path.toAbsolutePath() + "'; console.log(JSON.stringify(m." + function + "?.()))");
        });

        if (res.exitCode() != 0) {
            throw new RunException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }

        return res;
    }
}
