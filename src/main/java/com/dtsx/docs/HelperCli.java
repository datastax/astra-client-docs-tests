package com.dtsx.docs;

import com.dtsx.docs.commands.review.ReviewCmd;
import com.dtsx.docs.commands.run.RunCmd;
import com.dtsx.docs.commands.test.TestCmd;
import com.dtsx.docs.lib.CliLogger;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;

import static com.dtsx.docs.lib.ColorUtils.ACCENT_COLOR;

@Command(
    name = "dh",
    mixinStandardHelpOptions = true,
    subcommands = {
        TestCmd.class,
        RunCmd.class,
        ReviewCmd.class,
    }
)
public class HelperCli {
    public static void main(String[] args) {
        Dotenv.configure().systemProperties().ignoreIfMissing().load();

        val cli = new CommandLine(new HelperCli())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setColorScheme(new ColorScheme.Builder()
                .commands(ACCENT_COLOR)
                .options(ACCENT_COLOR)
                .parameters(ACCENT_COLOR)
                .optionParams(Style.italic)
                .build()
            )
            .setExecutionExceptionHandler((ex, _, _) -> {
                CliLogger.exception(ex);
                throw ex;
            });

        val exitCode = cli.execute(args);
        System.exit(exitCode);
    }
}
