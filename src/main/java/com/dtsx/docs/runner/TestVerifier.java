package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.TestResults.TestResult;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.GuidScrubber;
import org.approvaltests.scrubbers.MultiScrubber;
import org.approvaltests.scrubbers.RegExScrubber;
import org.graalvm.collections.Pair;

import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class TestVerifier {
    public static final Scrubber SCRUBBER = new MultiScrubber(List.of(
        new GuidScrubber(),
        new $DateScrubber()
    ));

    private final VerifierCtx ctx;

    public TestResult verify(TestMetadata md, RunResult result) {
        return verifySnapshot(md, mkSnapshot(md, result));
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

    private TestResult verifySnapshot(TestMetadata md, String snapshot) {
        val namer = new ExampleResultNamer(ctx, md);

        try {
            Approvals.verify(snapshot, mkApprovalOptions(md));
            return TestResult.passed(md, namer.getExampleName());
        } catch (Error e) {
            return TestResult.failed(md, namer.getExampleName(), e);
        }
    }

    private Options mkApprovalOptions(TestMetadata md) {
        return new Options()
            .forFile().withNamer(new ExampleResultNamer(ctx, md))
            .withScrubber(SCRUBBER)
            .withReporter((_, _) -> true);
    }

    private static class $DateScrubber extends RegExScrubber {
        public $DateScrubber() {
            super(Pattern.compile("\\{\\s*\"\\$date\"\\s*:\\s*\\d+\\s*}"), n -> "{ \"$date\" : \"date_" + n + "\" }");
        }
    }
}
