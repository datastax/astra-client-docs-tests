package com.dtsx.docs;

import com.dtsx.docs.builder.TestPlanBuilder;
import com.dtsx.docs.config.VerifierArgs;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.TestRunner;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(mixinStandardHelpOptions = true)
public class VerifierCli implements Runnable {
    @Mixin
    private VerifierArgs $args;

    public static void main(String[] args) {
        Dotenv.configure().systemProperties().ignoreIfMissing().load();
        val cli = new CommandLine(new VerifierCli()).setCaseInsensitiveEnumValuesAllowed(true);
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
