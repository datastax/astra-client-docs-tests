package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.Getter;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.writers.Writer;

import java.io.File;

public class ExampleResultNamer implements ApprovalNamer {
    @Getter
    private final String exampleName;
    private final String fileName;
    private final String additionalInformation;
    private final VerifierCtx ctx;

    public ExampleResultNamer(VerifierCtx ctx, ClientLanguage language, TestMetadata md) {
        this.exampleName = md.exampleFolder().getFileName().toString().split("\\.")[0];

        this.fileName = (!md.shareSnapshots())
            ? language.name().toLowerCase()
            : "shared";

        this.additionalInformation = "";
        this.ctx = ctx;
    }

    private ExampleResultNamer(VerifierCtx ctx, String exampleName, String fileName, String additionalInformation) {
        this.exampleName = exampleName;
        this.fileName = fileName;
        this.additionalInformation = additionalInformation;
        this.ctx = ctx;
    }

    @Override
    public String getApprovalName() {
        return exampleName + "/" + fileName + getAdditionalInformation();
    }

    @Override
    public String getSourceFilePath() {
        return ctx.snapshotsFolder().toAbsolutePath().toString();
    }

    @Override
    public File getReceivedFile(String extensionWithDot) {
        return new File(getSourceFilePath() + "/" + getApprovalName() + Writer.received + extensionWithDot);
    }

    @Override
    public File getApprovedFile(String extensionWithDot) {
        return new File(getSourceFilePath() + "/" + getApprovalName() + Writer.approved + extensionWithDot);
    }

    @Override
    public ApprovalNamer addAdditionalInformation(String additionalInformation) {
        return new ExampleResultNamer(this.ctx, this.exampleName, this.fileName, additionalInformation);
    }

    @Override
    public String getAdditionalInformation() {
        return additionalInformation;
    }
}
