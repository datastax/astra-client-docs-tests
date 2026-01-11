package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import com.dtsx.docs.runner.PlaceholderResolver;
import com.dtsx.docs.runner.TestRunException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@RequiredArgsConstructor
public final class JSFixtureImpl extends JSFixture {
    private final VerifierCtx ctx;
    private final Path path;
    private final boolean dryRun;

    @Override
    public String fixtureName() {
        return path.getFileName().toString();
    }

    @Override
    public FixtureMetadata meta(ExternalProgram tsx, Path nodePath) {
        // don't actually have fixture metadata here, but we can just pass the empty value
        // since it shouldn't be used outside of actual fixture functions
        val output = tryCallJsFunction(tsx, nodePath, FixtureMetadata.EMPTY, "Meta").stdout().trim();

        if (output.equals("undefined")) {
            return FixtureMetadata.EMPTY;
        }

        try {
            return JacksonUtils.parseJson(output, FixtureMetadata.class);
        } catch (Exception e) {
            throw new TestRunException("Failed to parse fixture metadata JSON from " + path + ":\n" + output, e);
        }
    }

    @Override
    protected void setup(ExternalProgram tsx, Path nodePath, FixtureMetadata md) {
        if (!dryRun) {
            tryCallJsFunction(tsx, nodePath, md, "Setup");
        }
    }

    @Override
    protected void reset(ExternalProgram tsx, Path nodePath, FixtureMetadata md) {
        if (!dryRun) {
            tryCallJsFunction(tsx, nodePath, md, "Reset");
        }
    }

    @Override
    protected void teardown(ExternalProgram tsx, Path nodePath, FixtureMetadata md) {
        if (!dryRun) {
            tryCallJsFunction(tsx, nodePath, md, "Teardown");
        }
    }

    @SneakyThrows
    private RunResult tryCallJsFunction(ExternalProgram tsx, Path nodePath, FixtureMetadata md, String function) {
        val displayPath = ctx.examplesFolder().relativize(path);

        if (!nodePath.endsWith("node_modules") || !Files.exists(nodePath)) {
            throw new TestRunException("Invalid dependencies directory '" + nodePath + "'"); // should never be thrown, but just in case
        }

        val envVars = new HashMap<>(PlaceholderResolver.mkEnvVars(ctx, md));
        envVars.put("NODE_PATH", nodePath.toString());

        // Calls the function if it exists
        val res = CliLogger.loading("Calling @!%s!@ in @!%s!@".formatted(function, displayPath), (_) -> {
            return tsx.run(null, envVars, "-e", "import * as m from '" + path.toAbsolutePath() + "'; console.log(JSON.stringify(m." + function + "?.()))");
        });

        if (res.exitCode() != 0) {
            throw new TestRunException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }

        return res;
    }
}
