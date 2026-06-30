package com.flowforge.service;

import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.Workflow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs workflows on a fixed background thread pool.
 * <p>
 * This makes multithreading explicit in the service layer: the Swing UI submits
 * work here and stays responsive, while several workflow runs may execute at
 * the same time on named daemon threads.
 */
public class WorkflowExecutionService implements AutoCloseable {

    private final WorkflowEngine engine;
    private final ExecutorService executor;

    public WorkflowExecutionService(WorkflowEngine engine) {
        this(engine, Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    }

    public WorkflowExecutionService(WorkflowEngine engine, int threads) {
        this.engine = engine;
        this.executor = Executors.newFixedThreadPool(Math.max(1, threads), new NamedThreadFactory());
    }

    public CompletableFuture<RunReport> runAsync(Workflow workflow, ExecutionContext context,
                                                 WorkflowExecutionListener listener) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return engine.run(workflow, context, listener);
            } catch (WorkflowValidationException e) {
                throw new WorkflowRunException(e);
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    /** Runtime wrapper so checked validation errors can cross CompletableFuture. */
    public static class WorkflowRunException extends RuntimeException {
        public WorkflowRunException(Throwable cause) {
            super(cause);
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "flowforge-runner-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
