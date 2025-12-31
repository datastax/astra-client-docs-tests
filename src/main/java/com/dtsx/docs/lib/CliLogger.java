package com.dtsx.docs.lib;

import com.dtsx.docs.config.VerifierCtx;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.experimental.Accessors;
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
import java.util.regex.Pattern;

import static com.dtsx.docs.VerifierCli.ACCENT_COLOR;

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
        log(String.join("", msg) + System.lineSeparator(), System.err);
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

    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("@!(.*?)!@");

    public static <T> T loading(@NonNull String rawInitialMsg, Function<Consumer<String>, T> supplier) {
        val initialMsg = HIGHLIGHT_PATTERN.matcher(rawInitialMsg)
            .replaceAll((match) -> highlight(match.group(1)));

        started(initialMsg);

        val isFirstLoading = globalSpinner.isEmpty();

        if (isFirstLoading && spinnerEnabled) {
            globalSpinner = Optional.of(new LoadingSpinner(initialMsg));
            globalSpinner.get().start();
        } else {
            globalSpinner.ifPresent(s -> s.pushMessage(initialMsg));
        }

        try {
            return supplier.apply((msg) -> {
                globalSpinner.ifPresent(s -> s.updateMessage(msg));
                accumulated.add("[LOADING:UPDATED] " + msg);
            });
        } finally {
            if (isFirstLoading) {
                globalSpinner.ifPresent(LoadingSpinner::stop);
                globalSpinner = Optional.empty();
            } else {
                globalSpinner.ifPresent(LoadingSpinner::popMessage);
            }
            done(initialMsg);
        }
    }

    public static String highlight(CharSequence s) {
        return ACCENT_COLOR.on() + s + ACCENT_COLOR.off();
    }

    private static void started(String... msg) {
        log("[STARTED] " + String.join("", msg), null);
    }

    private static void done(String... msg) {
        log("[DONE] " + String.join("", msg), null);
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
        accumulated.add(msg);

        if (ps == null) {
            return;
        }

        globalSpinner.ifPresent(LoadingSpinner::pause);

        try {
            ps.print(msg);
            ps.flush();
        } finally {
            globalSpinner.ifPresent(LoadingSpinner::resume);
        }
    }

    public static void dumpLogsToFile(VerifierCtx ctx) {
        try {
            val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(Instant.now().atZone(ZoneId.systemDefault()));
            val sessionLogFile = logsDir(ctx).resolve(timestamp + ".astra.log");

            Files.createDirectories(sessionLogFile.getParent());
            deleteOldLogs(logsDir(ctx));

            @Cleanup val writer = Files.newBufferedWriter(sessionLogFile);

            for (val line : accumulated) {
                writer.write(line);
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
                .skip(25)
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
