package info.kgeorgiy.ja.Davydov.iterative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import info.kgeorgiy.java.advanced.iterative.ListIP;

/**
 * Implementation of {@link ListIP} using manual thread management.
 */
public class IterativeParallelism implements ListIP {
    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
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

    private static <T, S> S computeRange(List<? extends T> list, int begin, int end, S state,
            IndexedAccumulator<? super T, ? super S> accumulator) {
        for (int i = begin; i < end; i++) {
            accumulator.apply(state, list.get(i), i);
        }
        return state;
    }

    private static <T, U, R> R parallelMap(int threads, List<? extends T> list,
            TriFunction<List<? extends T>, Integer, Integer, U> function, Function<List<U>, R> combiner)
            throws InterruptedException {
        int threadsCount = Math.min(Math.max(threads, 1), list.size());
        if (threadsCount == 0) {
            return combiner.apply(List.of());
        }
        List<Thread> threadList = new ArrayList<>();
        List<U> results = new ArrayList<>(Collections.nCopies(threadsCount, null));
        int pointer = 0;
        for (int i = 0; i < threadsCount; i++) {
            int end = pointer + (list.size() / threadsCount) + (i < list.size() % threadsCount ? 1 : 0);
            int begin = pointer;
            int ind = i;
            threadList.add(new Thread(() -> results.set(ind, function.apply(list, begin, end))));
            pointer = end;
        }
        for (Thread thread : threadList) {
            thread.start();
        }
        for (Thread thread : threadList) {
            thread.join();
        }
        return combiner.apply(results);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return parallelMap(threads, list, (a, b, c) -> {
            Box<Integer> maxIndex = new Box<>(b);
            computeRange(a, b + 1, c, maxIndex, (state, value, index) -> {
                if (comparator.compare(value, a.get(state.get())) > 0) {
                    state.set(index);
                }
            });
            return maxIndex.get();
        }, partResults -> partResults.stream().max(Comparator.comparing(list::get, comparator)).orElseThrow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, list, comparator.reversed());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(int threads, List<T> list, Predicate<? super T> predicate) throws InterruptedException {
        return parallelMap(threads, list, (a, b, c) -> {
            Box<Integer> firstIndex = new Box<>(-1);
            computeRange(a, b, c, firstIndex, (state, value, index) -> {
                if (state.get() == -1 && predicate.test(value)) {
                    state.set(index);
                }
            });
            return firstIndex.get();
        }, partResults -> partResults.stream().filter(index -> index >= 0).min(Integer::compareTo).orElse(-1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> list, Predicate<? super T> predicate) throws InterruptedException {
        return parallelMap(threads, list, (a, b, c) -> {
            Box<Integer> lastIndex = new Box<>(-1);
            computeRange(a, b, c, lastIndex, (state, value, index) -> {
                if (predicate.test(value)) {
                    state.set(index);
                }
            });
            return lastIndex.get();
        }, partResults -> partResults.stream().mapToInt(Integer::intValue).max().orElse(-1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelMap(threads, list, (a, b, c) -> {
            Box<Long> sum = new Box<>(0L);
            computeRange(a, b, c, sum, (state, value, index) -> {
                if (predicate.test(value)) {
                    state.set(state.get() + index);
                }
            });
            return sum.get();
        }, partResults -> partResults.stream().mapToLong(Long::longValue).sum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelMap(threads, list, (a, b, c) -> {
            List<T> part = new ArrayList<>();
            computeRange(a, b, c, part, (state, value, index) -> {
                if (predicate.test(value)) {
                    state.add(value);
                }
            });
            return part;
        }, partResults -> partResults.stream().flatMap(List::stream).toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelMap(threads, list, (a, b, c) -> {
            List<Integer> part = new ArrayList<>();
            computeRange(a, b, c, part, (state, value, index) -> {
                if (predicate.test(value)) {
                    state.add(index);
                }
            });
            return part;
        }, partResults -> partResults.stream().flatMap(List::stream).mapToInt(Integer::intValue).toArray());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function)
            throws InterruptedException {
        return parallelMap(threads, list, (a, b, c) -> {
            List<U> part = new ArrayList<>(c - b);
            computeRange(a, b, c, part, (state, value, index) -> state.add(function.apply(value)));
            return part;
        }, partResults -> partResults.stream().flatMap(List::stream).toList());
    }

}
