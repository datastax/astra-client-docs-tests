package com.dtsx.docs.runner.tests.snapshots.verifier;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.planner.TestRoot;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.Getter;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.writers.Writer;

import java.io.File;

public class ExampleResultNamer implements ApprovalNamer {
    @Getter
    private final String exampleName;
    private final String fileName;
    private final TestCtx ctx;

    public ExampleResultNamer(TestCtx ctx, ClientLanguage language, TestRoot testRoot, boolean shareSnapshots) {
        this.exampleName = testRoot.rootName();

        this.fileName = (!shareSnapshots)
            ? language.name().toLowerCase()
            : "shared";

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
        return this; // we can just ignore this
    }

    @Override
    public String getAdditionalInformation() {
        return ""; // we can just ignore this
    }
}
