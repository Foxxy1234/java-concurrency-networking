package info.kgeorgiy.ja.davydov.iterative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import info.kgeorgiy.java.advanced.iterative.*;
import info.kgeorgiy.java.advanced.mapper.*;

/**
 * Implementation of iterative parallelism contracts
 * <p>
 * The class works either via an external {@link ParallelMapper} (if provided),
 * or by creating worker threads for each parallel operation.
 *
 * @see NewListIP
 */
public class IterativeParallelism implements NewListIP {
    private final ParallelMapper parallelMapper;

    @FunctionalInterface
    private interface RangeFunction<ListT, BeginT, EndT, R> {
        R apply(ListT list, BeginT begin, EndT end);
    }

    @FunctionalInterface
    private interface IndexedAccumulator<T, S> {
        void apply(S state, T value, int index);
    }

    private static final class Box<T> {
        private T value;

        private Box(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    private static <T, S> S computeRange(List<? extends T> list, int begin, int end, int step, S state,
            IndexedAccumulator<T, S> accumulator) {
        for (int i = ((begin + step - 1) / step) * step; i < end; i += step) {
            accumulator.apply(state, list.get(i), i);
        }
        return state;
    }

    private static <T, S> S computeRangeIf(List<? extends T> list, int begin, int end, int step, S state,
            Predicate<? super T> predicate, IndexedAccumulator<T, S> accumulator) {
        return computeRange(list, begin, end, step, state, (curState, value, index) -> {
            if (predicate.test(value)) {
                accumulator.apply(curState, value, index);
            }
        });
    }

    private static <T> List<T> flattenLists(List<? extends List<? extends T>> partResults) {
        return partResults.stream().collect(ArrayList::new, List::addAll, List::addAll);
    }

    /**
     * Creates an instance without external mapper.
     */
    public IterativeParallelism() {
        this(null);
    }

    /**
     * Creates an instance backed by the provided mapper.
     *
     * @param parallelMapper mapper to delegate task execution to, or {@code null}
     *                           to use internal thread creation.
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private static int[] splitBorders(int size, int parts) {
        int[] borders = new int[parts + 1];
        int pointer = 0, plusOne = size % parts;
        for (int i = 0; i < parts; i++) {
            borders[i] = pointer;
            pointer += (size / parts) + (plusOne-- > 0 ? 1 : 0);
        }
        borders[parts] = pointer;
        return borders;
    }

    private void tryTerminate(Thread[] threads) {
        boolean interrupted = false;
        int index = 0;
        while (index < threads.length) {
            if (!threads[index].isAlive()) {
                index++;
                continue;
            }
            try {
                threads[index].join();
                index++;
            } catch (InterruptedException e) {
                if (!interrupted) {
                    interrupted = true;
                    for (Thread t : threads) {
                        t.interrupt();
                    }
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private <T, R> List<R> runMap(int threads, List<? extends T> list, Function<? super T, ? extends R> function)
            throws InterruptedException {
        if (list.isEmpty()) {
            return List.of();
        }
        if (parallelMapper != null) {
            return parallelMapper.map(function, list);
        }
        int threadsCount = Math.min(Math.max(threads, 1), list.size());
        Thread[] threadsArray = new Thread[threadsCount];
        RuntimeException[] exceptions = new RuntimeException[threadsCount];
        List<R> results = new ArrayList<>(Collections.nCopies(threadsCount, null));
        for (int i = 0; i < threadsCount; i++) {
            int ind = i;
            threadsArray[i] = new Thread(() -> {
                try {
                    results.set(ind, function.apply(list.get(ind)));
                } catch (RuntimeException e) {
                    exceptions[ind] = e;
                }
            });
        }
        for (Thread thread : threadsArray) {
            thread.start();
        }
        tryTerminate(threadsArray);
        RuntimeException toThrow = null;
        for (RuntimeException e : exceptions) {
            if (e != null) {
                if (toThrow == null) {
                    toThrow = e;
                } else {
                    toThrow.addSuppressed(e);
                }
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return results;
    }

    private <T, U, R> R parallelMap(int threads, List<? extends T> list,
            RangeFunction<List<? extends T>, Integer, Integer, U> function, Function<List<U>, R> combiner)
            throws InterruptedException {
        if (list.isEmpty()) {
            return combiner.apply(List.of());
        }
        int threadsCount = Math.max(threads, 1);
        int[] borders = splitBorders(list.size(), threadsCount);
        return combiner.apply(runMap(threadsCount, IntStream.range(0, threadsCount).boxed().toList(),
                index -> function.apply(list, borders[index], borders[index + 1])));
    }

    private <T, S, U, R> R parallelAccumulate(int threads, List<? extends T> list, int step, Supplier<S> stateFactory,
            IndexedAccumulator<T, S> accumulator, Function<? super S, ? extends U> extractor,
            Function<List<U>, R> combiner) throws InterruptedException {
        return parallelMap(threads, list,
                (a, b, c) -> extractor.apply(computeRange(a, b, c, step, stateFactory.get(), accumulator)), combiner);
    }

    private <T, S, U, R> R parallelAccumulateIf(int threads, List<? extends T> list, int step,
            Predicate<? super T> predicate, Supplier<S> stateFactory, IndexedAccumulator<T, S> accumulator,
            Function<? super S, ? extends U> extractor, Function<List<U>, R> combiner) throws InterruptedException {
        return parallelMap(threads, list,
                (a, b, c) -> extractor.apply(computeRangeIf(a, b, c, step, stateFactory.get(), predicate, accumulator)),
                combiner);
    }

    /**
     * {@inheritDoc}
     *
     * @see ScalarIP#argMax(int, List, Comparator)
     */
    @Override
    public <T> int argMax(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, list, comparator, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ScalarIP#argMin(int, List, Comparator)
     */
    @Override
    public <T> int argMin(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return argMin(threads, list, comparator, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ScalarIP#indexOf(int, List, Predicate)
     */
    @Override
    public <T> int indexOf(int threads, List<T> list, Predicate<? super T> predicate) throws InterruptedException {
        return indexOf(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ScalarIP#lastIndexOf(int, List, Predicate)
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> list, Predicate<? super T> predicate) throws InterruptedException {
        return lastIndexOf(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ScalarIP#sumIndices(int, List, Predicate)
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return sumIndices(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ListIP#filter(int, List, Predicate)
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return filter(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ListIP#indices(int, List, Predicate)
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return indices(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see ListIP#map(int, List, Function)
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function)
            throws InterruptedException {
        return map(threads, list, function, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see NewScalarIP#argMax(int, List, Comparator, int)
     */
    @Override
    public <T> int argMax(int threads, List<T> list, Comparator<? super T> comparator, int step)
            throws InterruptedException {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return parallelAccumulate(threads, list, step, () -> new Box<>(-1), (state, value, index) -> {
            if (state.get() == -1 || comparator.compare(value, list.get(state.get())) > 0) {
                state.set(index);
            }
        }, Box::get, partResults -> partResults.stream().filter(i -> i >= 0)
                .max(Comparator.comparing(list::get, comparator)).orElseThrow());
    }

    /**
     * {@inheritDoc}
     *
     * @see NewScalarIP#argMin(int, List, Comparator, int)
     */
    @Override
    public <T> int argMin(int threads, List<T> list, Comparator<? super T> comparator, int step)
            throws InterruptedException {
        return argMax(threads, list, comparator.reversed(), step);
    }

    /**
     * {@inheritDoc}
     *
     * @see NewScalarIP#indexOf(int, List, Predicate, int)
     */
    @Override
    public <T> int indexOf(int threads, List<T> list, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return parallelAccumulateIf(threads, list, step, predicate, () -> new Box<>(-1), (state, value, index) -> {
            if (state.get() == -1) {
                state.set(index);
            }
        }, Box::get,
                partResults -> partResults.stream().filter(index -> index >= 0).min(Integer::compareTo).orElse(-1));
    }

    /**
     * {@inheritDoc}
     *
     * @see NewScalarIP#lastIndexOf(int, List, Predicate, int)
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> list, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return parallelAccumulateIf(threads, list, step, predicate, () -> new Box<>(-1),
                (state, value, index) -> state.set(index), Box::get,
                partResults -> partResults.stream().mapToInt(Integer::intValue).max().orElse(-1));
    }

    /**
     * {@inheritDoc}
     *
     * @see NewScalarIP#sumIndices(int, List, Predicate, int)
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> list, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return parallelAccumulateIf(threads, list, step, predicate, () -> new Box<>(0L),
                (state, value, index) -> state.set(state.get() + index), Box::get,
                partResults -> partResults.stream().mapToLong(Long::longValue).sum());
    }

    /**
     * {@inheritDoc}
     *
     * @see NewListIP#filter(int, List, Predicate, int)
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return parallelAccumulateIf(threads, list, step, predicate, () -> new ArrayList<T>(),
                (state, value, index) -> state.add(value), Function.identity(), IterativeParallelism::flattenLists);
    }

    /**
     * {@inheritDoc}
     *
     * @see NewListIP#indices(int, List, Predicate, int)
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> list, Predicate<? super T> predicate, int step)
            throws InterruptedException {
        return parallelAccumulateIf(threads, list, step, predicate, () -> new ArrayList<Integer>(),
                (state, value, index) -> state.add(index), Function.identity(),
                partResults -> partResults.stream().flatMap(List::stream).mapToInt(Integer::intValue).toArray());
    }

    /**
     * {@inheritDoc}
     *
     * @see NewListIP#map(int, List, Function, int)
     */
    @Override
    public <T, R> List<R> map(int threads, List<? extends T> list, Function<? super T, ? extends R> function, int step)
            throws InterruptedException {
        return parallelAccumulate(threads, list, step, () -> new ArrayList<R>(),
                (state, value, index) -> state.add(function.apply(value)), Function.identity(),
                IterativeParallelism::flattenLists);
    }

}
