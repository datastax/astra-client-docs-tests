package com.dtsx.docs;

import com.dtsx.docs.builder.TestPlanBuilder;
import com.dtsx.docs.config.VerifierArgs;
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
        val plan = TestPlanBuilder.buildPlan(ctx);
        TestRunner.runTests(ctx, plan);
    }
}
