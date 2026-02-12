package com.dtsx.docs.lib;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;

public class ExecutorUtils {
    public enum DirectExecutor implements ExecutorService {
        INSTANCE;

        @Override
        public void shutdown() {}

        @Override
        public @NotNull List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(@NotNull Runnable command) {
            command.run();
        }

        @Override
        public <T> @NotNull Future<T> submit(@NotNull Callable<T> task) {
            val future = new FutureTask<>(task);
            future.run();
            return future;
        }

        @Override
        public <T> @NotNull Future<T> submit(@NotNull Runnable task, T result) {
            return submit(() -> { task.run(); return result; });
        }

        @Override
        public @NotNull Future<?> submit(@NotNull Runnable task) {
            return submit(task, null);
        }

        @Override
        public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) {
            val futures = new ArrayList<Future<T>>(tasks.size());
            for (val task : tasks) futures.add(submit(task));
            return futures;
        }

        @Override
        public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) {
            return invokeAll(tasks);
        }

        @Override
        public <T> @NotNull T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws ExecutionException {
            ExecutionException last = null;

            for (val task : tasks) {
                try { return task.call(); }
                catch (Exception e) { last = new ExecutionException(e); }
            }

            throw Objects.requireNonNullElseGet(last, () -> new ExecutionException("No tasks", null));
        }

        @Override
        public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws ExecutionException {
            return invokeAny(tasks);
        }
    }
}
