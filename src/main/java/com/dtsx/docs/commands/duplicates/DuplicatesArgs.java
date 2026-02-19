package com.dtsx.docs.commands.duplicates;

import com.dtsx.docs.config.args.BaseArgs;
import com.dtsx.docs.config.args.mixins.ExamplesFolderMixin;
import lombok.ToString;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

@ToString
public class DuplicatesArgs extends BaseArgs<DuplicatesCtx> {
    @Mixin
    public ExamplesFolderMixin $examplesFolder;

    @Override
    public DuplicatesCtx toCtx(CommandSpec spec) {
        return new DuplicatesCtx(this, spec);
    }
}
