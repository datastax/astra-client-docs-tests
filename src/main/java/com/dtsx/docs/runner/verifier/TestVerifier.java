package com.dtsx.docs.runner.verifier;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExampleResultNamer;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.approvers.FileApprover;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.GuidScrubber;
import org.approvaltests.scrubbers.MultiScrubber;
import org.approvaltests.scrubbers.RegExScrubber;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.dtsx.docs.runner.verifier.VerifyMode.DRY_RUN;

@RequiredArgsConstructor
public class TestVerifier {
    public static final Scrubber SCRUBBER = new MultiScrubber(List.of(
        new GuidScrubber(),
        new $DateScrubber()
    ));

    static {
        FileApprover.tracker.addAllowedDuplicates(_ -> true);
    }

    private final VerifierCtx ctx;

    public TestOutcome verify(ClientLanguage language, TestRoot testRoot, Supplier<RunResult> result) {
        if (ctx.verifyMode() == DRY_RUN) {
            return TestOutcome.Passed.INSTANCE;
        }
        return verifySnapshot(language, testRoot, mkSnapshot(testRoot, result.get()));
    }

    private String mkSnapshot(TestRoot testRoot, RunResult result) {
        val sb = new StringBuilder();

        for (val source : testRoot.snapshotters()) {
            val snapshot = source.mkSnapshot(ctx, result);
            sb.append("---").append(source.name().toLowerCase()).append("---\n");
            sb.append(snapshot).append("\n");
        }

        return sb.toString();
    }

    private TestOutcome verifySnapshot(ClientLanguage language, TestRoot testRoot, String snapshot) {
        try {
            Approvals.verify(snapshot, mkApprovalOptions(language, testRoot));
            return TestOutcome.Passed.INSTANCE;
        } catch (Error e) {
            return new TestOutcome.Failed(e);
        }
    }

    private Options mkApprovalOptions(ClientLanguage language, TestRoot testRoot) {
        val namer = mkNamer(language, testRoot);

        return new Options()
            .forFile().withNamer(namer)
            .withScrubber(SCRUBBER)
            .withReporter((_, _) -> true)
            .and(ctx.verifyMode().applyOptions(namer.getApprovedFile(".txt").toPath()));
    }

    private ExampleResultNamer mkNamer(ClientLanguage language, TestRoot testRoot) {
        return new ExampleResultNamer(ctx, language, testRoot);
    }

    private static class $DateScrubber extends RegExScrubber {
        public $DateScrubber() {
            super(Pattern.compile("\\{\\s*\"\\$date\"\\s*:\\s*\\d+\\s*}"), n -> "{ \"$date\" : \"date_" + n + "\" }");
        }
    }
}
