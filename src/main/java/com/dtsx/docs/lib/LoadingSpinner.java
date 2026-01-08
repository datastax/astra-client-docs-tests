package com.dtsx.docs.lib;

import lombok.val;
import org.apache.commons.lang3.function.FailableRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.dtsx.docs.lib.ColorUtils.stripAnsi;

// Vendored from Astra CLI
public class LoadingSpinner {
    private final String[] SPINNER_FRAMES = new String[]{ "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏" };
    private static final int FRAME_DELAY_MS = 80;

    private final AtomicReference<String> message;
    private final AtomicInteger lastLineLength = new AtomicInteger(0);

    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Object pauseLock = new Object();

    private final Thread spinnerThread;

    private LoadingSpinner(String message) {
        this.message = new AtomicReference<>(message);
        this.spinnerThread = Thread.ofVirtual().start(this::runSpinner);
    }

    public static LoadingSpinner start(String message) {
        return new LoadingSpinner(message);
    }

    public void stop() {
        spinnerThread.interrupt();
        try {
            spinnerThread.join(1000); // Wait max 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clearLine();
    }
    
    public void updateMessage(String newMessage) {
        message.set(newMessage);
    }

    public Runnable pause() {
        synchronized (pauseLock) {
            paused.set(true);
            // Wait for spinner to acknowledge pause
            try {
                pauseLock.wait(500); // Wait max 500ms for spinner to pause
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return () -> {
            synchronized (pauseLock) {
                paused.set(false);
                pauseLock.notifyAll();
            }
        };
    }

    private void runSpinner() {
        var frameIndex = 0;

        while (!Thread.currentThread().isInterrupted()) {
            if (paused.get()) {
                clearLine();

                synchronized (pauseLock) {
                    pauseLock.notifyAll();

                    while (paused.get()) {
                        catchInterrupt(pauseLock::wait);
                    }
                }
                continue;
            }

            clearLine();

            val currentLine = ColorUtils.highlight(SPINNER_FRAMES[frameIndex]) + " " + message.get() + "...";
            System.err.print(currentLine);
            System.err.flush();

            lastLineLength.set(stripAnsi(currentLine).length());

            frameIndex = (frameIndex + 1) % SPINNER_FRAMES.length;

            catchInterrupt(() -> Thread.sleep(FRAME_DELAY_MS));
        }
    }
    
    private void clearLine() {
        System.err.print("\r" + " ".repeat(lastLineLength.get()) + "\r");
        System.err.flush();
    }

    private void catchInterrupt(FailableRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}
