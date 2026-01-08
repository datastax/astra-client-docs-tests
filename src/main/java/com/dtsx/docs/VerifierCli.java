package com.dtsx.docs;

import com.dtsx.docs.builder.TestPlanBuilder;
import com.dtsx.docs.config.VerifierArgs;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ColorUtils;
import com.dtsx.docs.runner.TestRunner;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

import static com.dtsx.docs.lib.ColorUtils.ACCENT_COLOR;

@Command(mixinStandardHelpOptions = true)
public class VerifierCli implements Callable<Integer> {
    @Mixin
    private VerifierArgs $args;

    @Spec
    private CommandSpec spec;

    public static void main(String[] args) {
        Dotenv.configure().systemProperties().ignoreIfMissing().load();

        val cli = new CommandLine(new VerifierCli())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setColorScheme(new ColorScheme.Builder()
                .commands(ACCENT_COLOR)
                .options(ACCENT_COLOR)
                .parameters(ACCENT_COLOR)
                .optionParams(Style.italic)
                .build()
            );

        val exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        val ctx = $args.toCtx(spec);

        try {
            val ok = TestRunner.runTests(ctx, TestPlanBuilder.buildPlan(ctx));
            return (ok) ? 0 : 1;
        } finally {
            CliLogger.dumpLogsToFile(ctx);
        }
    }
}
