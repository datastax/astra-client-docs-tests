package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.builder.TestMetadata;
import lombok.Getter;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.writers.Writer;

import java.io.File;

public class ExampleResultNamer implements ApprovalNamer {
    @Getter
    private final String exampleName;
    private final String fileName;
    private final String additionalInformation;
    private final VerifierConfig cfg;

    public ExampleResultNamer(VerifierConfig cfg, TestMetadata md) {
        this.exampleName = md.exampleFolder().getFileName().toString().split("\\.")[0];

        this.fileName = (!md.shareSnapshots())
            ? cfg.driver().language().name().toLowerCase()
            : "shared";

        this.additionalInformation = "";
        this.cfg = cfg;
    }

    private ExampleResultNamer(VerifierConfig cfg, String exampleName, String fileName, String additionalInformation) {
        this.exampleName = exampleName;
        this.fileName = fileName;
        this.additionalInformation = additionalInformation;
        this.cfg = cfg;
    }

    @Override
    public String getApprovalName() {
        return exampleName + "/" + fileName + getAdditionalInformation();
    }

    @Override
    public String getSourceFilePath() {
        return cfg.snapshotsFolder().getAbsolutePath();
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
        return new ExampleResultNamer(this.cfg, this.exampleName, this.fileName, additionalInformation);
    }

    @Override
    public String getAdditionalInformation() {
        return additionalInformation;
    }
}
