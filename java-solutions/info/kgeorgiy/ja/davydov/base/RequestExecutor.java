package info.kgeorgiy.ja.davydov.base;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RequestExecutor implements AutoCloseable {
    private static final long LIGHT_SHUTDOWN_TIMEOUT = 200L;
    private static final long HARD_SHUTDOWN_TIMEOUT = 400L;

    private final ThreadPoolExecutor executor;

    /**
     * Creates a fixed-size executor for crawler tasks.
     *
     * @param threads       number of worker threads
     * @param queueCapacity task queue capacity; when full, submitter blocks until
     *                          queue space is available
     */
    public RequestExecutor(int threads, int queueCapacity) {
        this.executor = new ThreadPoolExecutor(threads, threads, 5L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity));
        this.executor.setRejectedExecutionHandler((task, rejectedFrom) -> {
            try {
                rejectedFrom.getQueue().put(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while waiting for free queue slot", e);
            }
        });
    }

    /**
     * Submits a task for asynchronous execution.
     *
     * @param task task to execute
     * @param <T>  task result type
     * @return future representing pending completion of the task
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * Submits a runnable task for asynchronous execution.
     *
     * @param task task to execute
     * @return future representing pending completion of the task
     */
    public Future<?> run(Runnable task) {
        return executor.submit(task);
    }

    /**
     * Waits for completion of a previously submitted task.
     *
     * @param future task future
     * @param <T>    task result type
     * @return computed task result
     * @throws InterruptedException if interrupted while waiting
     */
    public <T> T await(Future<T> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unexpected task failure", e.getCause());
        }
    }

    /**
     * Submits a task and waits for its result.
     *
     * @param task task to execute
     * @param <T>  task result type
     * @return computed task result
     * @throws InterruptedException if interrupted while waiting
     */
    public <T> T runAndAwait(Callable<T> task) throws InterruptedException {
        return await(submit(task));
    }

    /**
     * Submits a runnable task and waits for its completion.
     *
     * @param task task to execute
     * @throws InterruptedException if interrupted while waiting
     */
    public void runAndAwait(Runnable task) throws InterruptedException {
        await(run(task));
    }

    /**
     * Initiates graceful shutdown and waits for worker termination.
     * 
     * @throws RuntimeException if couldn't close executor in a reasonable amount of
     *                              time
     */
    @Override
    public void close() throws RuntimeException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(LIGHT_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(HARD_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Couldn't close the queue in a reasonable amount of time");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
