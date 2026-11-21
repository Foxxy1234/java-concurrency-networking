package online.foxxy239.concurrentnetworking.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public final class ParallelMapper implements AutoCloseable {
    private final ExecutorService workers;

    public ParallelMapper(final int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be positive");
        }
        workers = Executors.newFixedThreadPool(threads);
    }

    public <T, R> List<R> map(final Function<? super T, ? extends R> function, final List<? extends T> values)
            throws Exception {
        final List<Future<R>> futures = new ArrayList<>(values.size());
        for (final T value : values) {
            futures.add(workers.submit(() -> function.apply(value)));
        }
        final List<R> result = new ArrayList<>(values.size());
        for (final Future<R> future : futures) {
            result.add(future.get());
        }
        return result;
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
