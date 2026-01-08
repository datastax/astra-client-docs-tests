package com.dtsx.docs.lib;

import com.dtsx.docs.config.VerifierCtx;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dtsx.docs.lib.ColorUtils.stripAnsi;

// Adapted from Astra CLI
public class CliLogger {
    private static final List<String> accumulated = Collections.synchronizedList(new ArrayList<>());

    private static Optional<LoadingSpinner> globalSpinner = Optional.empty();
    private static boolean spinnerEnabled;

    public static void setSpinnerEnabled(boolean spinnerEnabled) {
        CliLogger.spinnerEnabled = spinnerEnabled;
    }

    public static void print(String... msg) {
        log(String.join("", msg), System.out);
    }

    public static void println(String... msg) {
        log(String.join("", msg) + System.lineSeparator(), System.out);
    }

    public static void error(String... msg) {
        log(String.join("", msg), System.out);
    }

    public static void errorln(String... msg) {
        log(String.join("", msg) + System.lineSeparator(), System.err);
    }

    public static void debug(String... msg) {
        log("[DEBUG] " + String.join("", msg), null);
    }

    public static <T> T loading(@NonNull String initialMsg, Function<Consumer<String>, T> supplier) {
        val formattedMsg = ColorUtils.format(initialMsg);

        accumulated.add("[LOADING:STARTED] " + formattedMsg);

        val isFirstLoading = globalSpinner.isEmpty();

        if (isFirstLoading && spinnerEnabled) {
            globalSpinner = Optional.of(LoadingSpinner.start(formattedMsg));
        }

        try {
            return supplier.apply((msg) -> {
                globalSpinner.ifPresent(s -> s.updateMessage(ColorUtils.format(msg)));
                accumulated.add("[LOADING:UPDATED] " + msg);
            });
        } finally {
            if (isFirstLoading) {
                globalSpinner.ifPresent(LoadingSpinner::stop);
                globalSpinner = Optional.empty();
            }
            accumulated.add("[LOADING:DONE] " + formattedMsg);
        }
    }

    public static void exception(String... msg) {
        log("[ERROR] " + String.join("", msg), System.err);
    }

    public static <E extends Throwable> E exception(String msg, E e) {
        exception(msg);
        return exception(e);
    }

    public static <E extends Throwable> E exception(E e) {
        exception(captureStackTrace(e));
        return e;
    }

    private static void log(String msg, @Nullable PrintStream ps) {
        val formattedMsg = ColorUtils.format(msg);

        accumulated.add(formattedMsg);

        if (ps == null) {
            return;
        }

        val resume = globalSpinner.map(LoadingSpinner::pause);

        try {
            ps.print(formattedMsg);
            ps.flush();
        } finally {
            resume.ifPresent(Runnable::run);
        }
    }

    public static void dumpLogsToFile(VerifierCtx ctx) {
        try {
            val timestamp = DateTimeFormatter.ofPattern("yyyy_MM_dd___H_mm_ss").format(Instant.now().atZone(ZoneId.systemDefault()));
            val sessionLogFile = logsDir(ctx).resolve(timestamp + ".astra.log");

            Files.createDirectories(sessionLogFile.getParent());
            deleteOldLogs(logsDir(ctx));

            @Cleanup val writer = Files.newBufferedWriter(sessionLogFile);

            for (val line : accumulated) {
                writer.write(stripAnsi(line));
                writer.write(System.lineSeparator());
            }
        } catch (Exception _) {}
    }

    private static void deleteOldLogs(Path logsDir) {
        try {
            @Cleanup val logFiles = Files.list(logsDir);

            logFiles
                .sorted(Comparator.<Path, FileTime>comparing((path) -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).reversed())
                .skip(10)
                .forEach((f) -> {
                    try {
                        Files.delete(f);
                    } catch (Exception e) {
                        exception("Error deleting old log file '", f.toString(), "': ", e.getMessage());
                    }
                });
        } catch (Exception _) {}
    }

    private static String captureStackTrace(Throwable t) {
        val sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static Path logsDir(VerifierCtx ctx) {
        return ctx.tmpFolder().resolve("logs");
    }
}
