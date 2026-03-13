package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.Spliterator;

public abstract class AbstractSizedTreeSpliterator<T, N> extends AbstractTreeSpliterator<T, N> {
    protected static class SizeCounter {
        long size;

        SizeCounter(long size) {
            this.size = size;
        }
    }

    private final SizeCounter counter;

    protected AbstractSizedTreeSpliterator(ArrayDeque<N> deque, SizeCounter counter, int baseCharacteristics) {
        super(deque, baseCharacteristics);
        this.counter = counter;
    }

    protected abstract long nodeSize(N node);

    protected abstract AbstractSizedTreeSpliterator<T, N> createSizedChild(ArrayDeque<N> childDeque,
            SizeCounter childCounter);

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

        while (0 < middle && !deque.isEmpty()) {
            N first = deque.peek();
            long firstSize = isLeaf(first) ? 1 : nodeSize(first);

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
