package com.dtsx.docs.commands.run;

import com.dtsx.docs.commands.BaseCmd;
import com.dtsx.docs.runner.scripts.ScriptRunner;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "run",
    mixinStandardHelpOptions = true,
    description = "Run script files without testing them."
)
public class RunCmd extends BaseCmd<RunCtx> {
    @Mixin @Getter
    private RunArgs $args;

    @Override
    public int run() {
        return ScriptRunner.runScripts(ctx)
            ? 0
            : 1;
    }
}
