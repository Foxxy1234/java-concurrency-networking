package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.java.advanced.streams.Trees.SizedBinary;

public class SizedTreeSpliterator<T, N> extends AbstractTreeSpliterator<T, N> {
    private static final int BASE_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;

    private static class SizeCounter {
        long size;

        SizeCounter(long size) {
            this.size = size;
        }
    }

    private final Function<? super N, ? extends T> valueExtractor;
    private final BiConsumer<ArrayDeque<N>, N> branchTraverser;
    private final ToLongFunction<? super N> nodeSizer;
    private final SizeCounter counter;

    private SizedTreeSpliterator(ArrayDeque<N> deque, Function<? super N, ? extends T> valueExtractor,
            BiConsumer<ArrayDeque<N>, N> branchTraverser, ToLongFunction<? super N> nodeSizer, SizeCounter counter) {
        super(deque, BASE_CHARACTERISTICS);
        this.valueExtractor = valueExtractor;
        this.branchTraverser = branchTraverser;
        this.nodeSizer = nodeSizer;
        this.counter = counter;
    }

    public static <T> SizedTreeSpliterator<T, SizedBinary<T>> binary(SizedBinary<T> tree) {
        return new SizedTreeSpliterator<>(initDeque(tree), leaf -> ((Trees.Leaf<T>) leaf).value(), (queue, branch) -> {
            SizedBinary.Branch<T> b = (SizedBinary.Branch<T>) branch;
            if (b.right() != null) {
                queue.push(b.right());
            }
            if (b.left() != null) {
                queue.push(b.left());
            }
        }, SizedBinary::size, new SizeCounter(tree != null ? tree.size() : 0));
    }

    @Override
    protected T getValue(N leaf) {
        return valueExtractor.apply(leaf);
    }

    @Override
    protected void traverseBranch(ArrayDeque<N> queue, N branch) {
        branchTraverser.accept(queue, branch);
    }

    private SizedTreeSpliterator<T, N> createSizedChild(ArrayDeque<N> childDeque, SizeCounter childCounter) {
        return new SizedTreeSpliterator<>(childDeque, valueExtractor, branchTraverser, nodeSizer, childCounter);
    }

    @Override
    protected void afterLeafVisit() {
        counter.size--;
    }

    @Override
    public Spliterator<T> trySplit() {
        if (counter.size < 2) {
            return null;
        }
        long middle = counter.size / 2;
        ArrayDeque<N> childQueue = new ArrayDeque<>();
        while (middle > 0 && !deque.isEmpty()) {
            N first = deque.peek();
            long firstSize = isLeaf(first) ? 1 : nodeSizer.applyAsLong(first);
            if (firstSize <= middle) {
                deque.pop();
                childQueue.addLast(first);
                middle -= firstSize;
            } else {
                deque.pop();
                traverseBranch(deque, first);
            }
        }
        if (childQueue.isEmpty()) {
            return null;
        }
        long taken = counter.size / 2 - middle;
        counter.size -= taken;
        return createSizedChild(childQueue, new SizeCounter(taken));
    }

    @Override
    public long estimateSize() {
        return counter.size;
    }

    @Override
    public int characteristics() {
        return super.characteristics() | Spliterator.SIZED | Spliterator.SUBSIZED;
    }
}
