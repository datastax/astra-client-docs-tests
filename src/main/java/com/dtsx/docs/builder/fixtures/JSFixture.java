package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.verifier.VerifyMode;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public sealed interface JSFixture permits NoopFixture, JSFixtureImpl {
    String fixtureName();

    void setup(ExternalProgram tsx);
    void reset(ExternalProgram tsx);
    void teardown(ExternalProgram tsx);

    static JSFixture mkFor(VerifierCtx ctx, Path path) {
        if (!Files.exists(path)) {
            return NoopFixture.INSTANCE;
        }

        return (ctx.verifyMode() != VerifyMode.DRY_RUN)
            ? new JSFixtureImpl(ctx, path)
            : new NoopFixture(path.getFileName().toString());
    }

    default <T> void useResetting(ExternalProgram tsx, Iterable<T> t, Consumer<T> consumer) {
        setup(tsx);
        try {
            for (val item : t) {
                reset(tsx);
                consumer.accept(item);
            }
        } finally {
            teardown(tsx);
        }
    }

    static void installDependencies(VerifierCtx ctx) {
        val res = CliLogger.loading("Installing JS fixture dependencies...", (_) -> {
            return ExternalPrograms.npm(ctx).run("install");
        });

        if (res.exitCode() != 0) {
            throw new IllegalStateException("Failed to install JS fixture dependencies: " + res.output());
        }
    }
}
