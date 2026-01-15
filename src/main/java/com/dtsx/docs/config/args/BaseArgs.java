package com.dtsx.docs.config.args;

import com.dtsx.docs.config.ctx.BaseCtx;
import com.dtsx.docs.lib.CliLogger;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

public abstract class BaseArgs<Ctx extends BaseCtx> {
    @Option(
        names = { "--spinner" },
        description = "enable/disable spinner in the CLI output.",
        defaultValue = "${SPINNER:-true}",
        fallbackValue = "true",
        negatable = true
    )
    public void $spinner(boolean enabled) {
        CliLogger.setSpinnerEnabled(enabled);
    }

    public abstract Ctx toCtx(CommandSpec spec);
}
