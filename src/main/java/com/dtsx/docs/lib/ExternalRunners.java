package com.dtsx.docs.lib;

import com.dtsx.docs.VerifierConfig;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;

public class ExternalRunners {
    public record RunResult(int exitCode, String stdout, String stderr) {}

    public record ExternalRunner(VerifierConfig cfg, Function<VerifierConfig, String[]> cmd) {
        public RunResult run(String... args) {
            val baseCmd = cmd.apply(cfg);
            val fullCmd = new String[baseCmd.length + args.length];
            System.arraycopy(baseCmd, 0, fullCmd, 0, baseCmd.length);
            System.arraycopy(args, 0, fullCmd, baseCmd.length, args.length);

            val pb = new ProcessBuilder(fullCmd);
            pb.redirectErrorStream(false);

            try {
                val process = pb.start();

                val stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .reduce("", (a, b) -> a + b + "\n");

                val stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines()
                    .reduce("", (a, b) -> a + b + "\n");

                val exitCode = process.waitFor();
                return new RunResult(exitCode, stdout, stderr);

            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt
                return new RunResult(-1, "", e.getMessage());
            }
        }

        public boolean exists() {
            val baseCmd = cmd.apply(cfg);

            try {
                val pb = new ProcessBuilder(baseCmd[0], "--version");
                val process = pb.start();
                val exitCode = process.waitFor();
                return exitCode == 0;
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public static ExternalRunner tsx(VerifierConfig ctx) {
        return new ExternalRunner(ctx, cfg -> cfg.commands.tsx());
    }
}
