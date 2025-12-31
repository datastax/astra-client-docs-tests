package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.TestResults.TestResult;
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
import org.graalvm.collections.Pair;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

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

    public TestResult verify(ClientLanguage language, TestMetadata md, Path exampleFile, RunResult result) {
        return verifySnapshot(language, md, exampleFile, mkSnapshot(md, result));
    }

    private String mkSnapshot(TestMetadata md, RunResult result) {
        return md.snapshotters().stream()
            .map((snapper) -> Pair.create(snapper, snapper.mkSnapshot(ctx, result)))
            .reduce(
                "",
                (acc, pair) -> acc + "---" + pair.getLeft().name().toLowerCase() + "---\n" + pair.getRight() + "\n",
                (a, b) -> a + b
            );
    }

    private TestResult verifySnapshot(ClientLanguage language, TestMetadata md, Path exampleFile, String snapshot) {
        val namer = mkNamer(language, md);
        val displayPath = ctx.examplesFolder().relativize(exampleFile).toString();

        try {
            Approvals.verify(snapshot, mkApprovalOptions(language, md));
            return TestResult.passed(md, displayPath, namer.getExampleName());
        } catch (Error e) {
            return TestResult.failed(md, displayPath, namer.getExampleName(), e);
        }
    }

    private Options mkApprovalOptions(ClientLanguage language, TestMetadata md) {
        return new Options()
            .forFile().withNamer(mkNamer(language, md))
            .withScrubber(SCRUBBER)
            .withReporter((_, _) -> true);
    }

    private ExampleResultNamer mkNamer(ClientLanguage language, TestMetadata md) {
        return new ExampleResultNamer(ctx, language, md);
    }

    private static class $DateScrubber extends RegExScrubber {
        public $DateScrubber() {
            super(Pattern.compile("\\{\\s*\"\\$date\"\\s*:\\s*\\d+\\s*}"), n -> "{ \"$date\" : \"date_" + n + "\" }");
        }
    }
}
