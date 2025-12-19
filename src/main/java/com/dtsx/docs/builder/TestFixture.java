package com.dtsx.docs.builder;

import com.dtsx.docs.lib.ExternalRunners.ExternalRunner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.file.Path;

@ToString(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
@RequiredArgsConstructor
public class TestFixture {
    @Getter
    @ToString.Include
    private final Path path;

    public void setup(ExternalRunner tsx) {
        tryCallJsFunction(tsx, "Setup");
    }

    public void reset(ExternalRunner tsx) {
        tryCallJsFunction(tsx, "Reset");
    }

    public void teardown(ExternalRunner tsx) {
        tryCallJsFunction(tsx, "Teardown");
    }

    private void tryCallJsFunction(ExternalRunner tsx, String function) {
        val res = tsx.run("-e", "import * as m from '" + path.toAbsolutePath().toString() + "'; await m." + function + "?.()");

        if (res.exitCode() != 0) {
            throw new RuntimeException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }
    }
}
