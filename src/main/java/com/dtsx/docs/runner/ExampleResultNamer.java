package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.Getter;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.writers.Writer;

import java.io.File;

public class ExampleResultNamer implements ApprovalNamer {
    @Getter
    private final String exampleName;
    private final String fileName;
    private final ClientLanguage language;
    private final String additionalInformation;
    private final VerifierCtx ctx;

    public ExampleResultNamer(VerifierCtx ctx, ClientLanguage language, TestRoot testRoot) {
        this.exampleName = testRoot.path().getFileName().toString().split("\\.")[0];

        this.fileName = (!testRoot.shareSnapshots())
            ? language.name().toLowerCase()
            : "shared";

        this.language = language;
        this.additionalInformation = "";
        this.ctx = ctx;
    }

    private ExampleResultNamer(VerifierCtx ctx, String exampleName, String fileName, ClientLanguage language, String additionalInformation) {
        this.exampleName = exampleName;
        this.fileName = fileName;
        this.language = language;
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
        return new ExampleResultNamer(this.ctx, this.exampleName, this.fileName, this.language, additionalInformation);
    }

    @Override
    public String getAdditionalInformation() {
        return additionalInformation;
    }
}
