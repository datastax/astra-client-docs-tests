package com.dtsx.docs.lib;

import com.dtsx.docs.VerifierConfig;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;

public class ExternalPrograms {
    public sealed interface OutputLine { String unwrap(); }
    public record StdoutLine(String unwrap) implements OutputLine {}
    public record StderrLine(String unwrap) implements OutputLine {}

    public record RunResult(int exitCode, List<OutputLine> outputLines) {
        public String stdout() {
            return buildOutput(StdoutLine.class::isInstance);
        }

        public String stderr() {
            return buildOutput(StderrLine.class::isInstance);
        }

        public String output() {
            return buildOutput(_ -> true);
        }

        private String buildOutput(Predicate<OutputLine> filter) {
            val sb = new StringBuilder();
            for (val line : outputLines) {
                if (filter.test(line)) {
                    sb.append(line.unwrap());
                }
            }
            return sb.toString();
        }
    }

    public static ExternalProgram tsx(VerifierConfig ctx) {
        return new ExternalProgram("tsx", ctx, cfg -> cfg.commands.tsx());
    }

    public static ExternalProgram npm(VerifierConfig ctx) {
        return new ExternalProgram("npm", ctx, cfg -> cfg.commands.npm());
    }

    public static ExternalProgram bash(VerifierConfig ctx) {
        return new ExternalProgram("bash", ctx, cfg -> cfg.commands.npm());
    }

    public record ExternalProgram(String name, VerifierConfig cfg, Function<VerifierConfig, String[]> cmd) {
        public RunResult run(String... args) {
            return run(null, args);
        }

        public RunResult run(@Nullable Path dir, String... args) {
            val pb = mkProcessBuilder(dir, ArrayUtils.addAll(cmd.apply(cfg), args));

            try {
                val process = pb.start();

                val outputLines = Collections.synchronizedList(new ArrayList<OutputLine>());

                try (var executor = Executors.newFixedThreadPool(2)) {
                    executor.submit(
                        mkStreamReader(process.getInputStream(), outputLines, StdoutLine::new)
                    );
                    executor.submit(
                        mkStreamReader(process.getErrorStream(), outputLines, StderrLine::new)
                    );
                    return new RunResult(process.waitFor(), outputLines);
                }

            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return new RunResult(-1, List.of(new StderrLine(e.toString())));
            }
        }

        private ProcessBuilder mkProcessBuilder(Path dir, String[] fullCmd) {
            val pb = new ProcessBuilder(fullCmd);

            if (dir != null) {
                pb.directory(dir.toFile());
            }

            return pb;
        }

        private Runnable mkStreamReader(InputStream stream, List<OutputLine> outputLines, Function<String, OutputLine> mapper) {
            return () -> {
                try (var reader = new BufferedReader(new InputStreamReader(stream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLines.add(mapper.apply(line + System.lineSeparator()));
                    }
                } catch (IOException ignored) {}
            };
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

        public String envVar() {
            return name.toUpperCase() + "_COMMAND";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ExternalProgram other && this.name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
