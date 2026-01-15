package com.dtsx.docs.commands.run;

import com.dtsx.docs.config.args.BaseScriptRunnerArgs;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Optional;

public class RunArgs extends BaseScriptRunnerArgs<RunCtx> {
    @Parameters(
        description = "Files to run.",
        paramLabel = "FILE"
    )
    public List<String> $scripts;

    @Option(
        names = { "-c", "--collection" },
        description = "Collection name for placeholder replacement.",
        paramLabel = "NAME"
    )
    public Optional<String> $collection;

    @Option(
        names = { "--table" },
        description = "Table name for placeholder replacement.",
        paramLabel = "NAME"
    )
    public Optional<String> $table;

    @Option(
        names = { "-k", "--keyspace" },
        description = "Keyspace name for placeholder replacement.",
        defaultValue = "default_keyspace",
        paramLabel = "NAME"
    )
    public String $keyspace;

    @Option(
        names = { "-p", "--plain" },
        description = "Plain output without colors or formatting. Only works with a single script file."
    )
    public boolean $plain;

    @Override
    public RunCtx toCtx(CommandSpec spec) {
        return new RunCtx(this, spec);
    }
}
