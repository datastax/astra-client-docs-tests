package com.dtsx.docs.commands.duplicates;

import com.dtsx.docs.config.ctx.BaseCtx;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

import java.nio.file.Path;

@Getter
public class DuplicatesCtx extends BaseCtx {
    private final Path examplesFolder;

    public DuplicatesCtx(DuplicatesArgs args, CommandSpec spec) {
        super(args, spec);
        this.examplesFolder = args.$examplesFolder.resolve();
    }
}
