package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.builder.TestMetadata;
import lombok.val;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.writers.Writer;

import java.io.File;

public class ExampleResultNamer implements ApprovalNamer {
    private final String name;
    private final String additionalInformation;
    private final VerifierConfig cfg;

    public ExampleResultNamer(VerifierConfig cfg, TestMetadata md) {
        val exampleFileName = md.exampleFile().getFileName().toString();

        this.name = (!md.shareSnapshots())
            ? cfg.driver().language().name().toLowerCase() + ":" + exampleFileName
            : exampleFileName;

        this.additionalInformation = "";
        this.cfg = cfg;
    }

    private ExampleResultNamer(VerifierConfig cfg, String name, String additionalInformation) {
        this.name = name;
        this.additionalInformation = additionalInformation;
        this.cfg = cfg;
    }

    @Override
    public String getApprovalName() {
        return name + getAdditionalInformation();
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

    public ApprovalNamer addAdditionalInformation(String additionalInformation) {
        return new ExampleResultNamer(this.cfg, this.name, additionalInformation);
    }

    @Override
    public String getAdditionalInformation() {
        return additionalInformation;
    }
}

