package com.dtsx.docs.lib;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.runner.SourceCodeReplacer;
import lombok.RequiredArgsConstructor;
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

import static com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType.*;

public class ExternalPrograms {
    public static ExternalProgram tsx(VerifierCtx ctx) {
        return get(TSX, ctx);
    }

    public static ExternalProgram npm(VerifierCtx ctx) {
        return get(NPM, ctx);
    }

    public static ExternalProgram bash(VerifierCtx ctx) {
        return get(BASH, ctx);
    }

    public static ExternalProgram python(VerifierCtx ctx) {
        return get(PYTHON, ctx);
    }

    public static ExternalProgram java(VerifierCtx ctx) {
        return get(JAVA, ctx);
    }

    public static ExternalProgram custom(VerifierCtx ctx) {
        return new ExternalProgram(ctx, "custom", new String[0]);
    }

    private static ExternalProgram get(ExternalProgramType type, VerifierCtx ctx) {
        return new ExternalProgram(ctx, type.name().toLowerCase(), ctx.commandOverrides().getOrDefault(type, type.defaultCommand()));
    }

    @RequiredArgsConstructor
    public enum ExternalProgramType {
        TSX("npx tsx"),
        NPM("npm"),
        BASH("bash"),
        PYTHON("python3"),
        JAVA("java");

        private final String defaultCommand;

        public String[] defaultCommand() {
            return defaultCommand.split(" ");
        }
    }

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

    public record ExternalProgram(VerifierCtx ctx, String name, String[] cmd) {
        public RunResult run(String... args) {
            return run(null, args);
        }

        public RunResult run(@Nullable Path dir, String... args) {
            val pb = mkProcessBuilder(dir, ArrayUtils.addAll(cmd, args));

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

            pb.environment().putAll(SourceCodeReplacer.mkEnvVars(ctx));

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
            try {
                val pb = new ProcessBuilder(ArrayUtils.addAll(cmd, "--version"));
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
