package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.Spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.java.advanced.streams.Trees.SizedBinary;

public class SizedBinaryTreeSpliterator<T> extends AbstractSizedTreeSpliterator<T, SizedBinary<T>> {
    private static final int BASE_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;

    public SizedBinaryTreeSpliterator(SizedBinary<T> tree) {
        this(initDeque(tree), new SizeCounter(tree != null ? tree.size() : 0));
    }

    private SizedBinaryTreeSpliterator(ArrayDeque<SizedBinary<T>> deque, SizeCounter counter) {
        super(deque, counter, BASE_CHARACTERISTICS);
    }

    @Override
    protected T getValue(SizedBinary<T> leaf) {
        return ((Trees.Leaf<T>) leaf).value();
    }

    @Override
    protected void traverseBranch(ArrayDeque<SizedBinary<T>> queue, SizedBinary<T> branch) {
        SizedBinary.Branch<T> b = (SizedBinary.Branch<T>) branch;
        if (b.right() != null) {
            queue.push(b.right());
        }
        if (b.left() != null) {
            queue.push(b.left());
        }
    }

    @Override
    protected long nodeSize(SizedBinary<T> node) {
        return node.size();
    }

    @Override
    protected AbstractSizedTreeSpliterator<T, SizedBinary<T>> createSizedChild(ArrayDeque<SizedBinary<T>> childQueue,
            SizeCounter childCounter) {
        return new SizedBinaryTreeSpliterator<>(childQueue, childCounter);
    }
}
