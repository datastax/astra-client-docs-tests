package com.dtsx.docs.config.ctx;

import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.nio.file.Path;

@Getter
public abstract class BaseCtx {
    protected CommandLine cmd;

    /// Always `.docs_tests_temp/` - contains execution environments and intermediate files.
    ///
    /// Deleted on startup if `--clean` is set, and optionally deleted after tests complete.
    private final Path tmpFolder;

    public BaseCtx(CommandSpec spec) {
        this.cmd = spec.commandLine();
        this.tmpFolder = Path.of(".docs_tests_temp");
    }
}
