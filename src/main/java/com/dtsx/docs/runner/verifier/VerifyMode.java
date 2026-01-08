package com.dtsx.docs.runner.verifier;

import com.dtsx.docs.runner.TestRunException;
import org.approvaltests.core.Options;
import org.approvaltests.inline.InlineOptions;
import org.lambda.functions.Function1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public enum VerifyMode {
    NORMAL,
    VERIFY_ONLY,
    DRY_RUN;

    public Function1<Options, Options> applyOptions(Path approvedFile) {
        return switch (this) {
            case NORMAL -> {
                yield (o) -> o;
            }
            case VERIFY_ONLY -> (o) -> {
                if (!Files.exists(approvedFile)) {
                    return o.inline("");
                }

                try {
                    return o.inline(Files.readString(approvedFile), InlineOptions.showCode(false));
                } catch (IOException e) {
                    throw new TestRunException("Failed to read example file for VERIFY_ONLY verification mode: " + approvedFile, e);
                }
            };
            case DRY_RUN -> {
                throw new TestRunException("DRY_RUN mode should not apply verification options; it should've skipped verification entirely.");
            }
        };
    }
}
