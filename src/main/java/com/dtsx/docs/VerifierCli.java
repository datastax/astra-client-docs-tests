package com.dtsx.docs;

import com.dtsx.docs.builder.TestPlanBuilder;
import com.dtsx.docs.config.VerifierArgs;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.TestRunner;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi.IStyle;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Mixin;

@Command(mixinStandardHelpOptions = true)
public class VerifierCli implements Runnable {
    @Mixin
    private VerifierArgs $args;

    public static IStyle ACCENT_COLOR = new IStyle() {
        public String on() { return CSI + "38;5;110m"; }
        public String off() { return CSI + "0m"; }
    };

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

        cli.execute(args);
    }

    @Override
    public void run() {
        val ctx = $args.toCtx();

        try {
            val plan = CliLogger.loading("Building test plan...", (_) ->
                TestPlanBuilder.buildPlan(ctx)
            );

            if ($args.$dryRun) {
                CliLogger.println("Tests to be executed (dry run):\n\n" + plan);
            } else {
                TestRunner.runTests(ctx, plan);
            }
        } finally {
            CliLogger.dumpLogsToFile(ctx);
        }
    }
}
