package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.Spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.java.advanced.streams.Trees.Binary;

public class BinaryTreeSpliterator<T> extends AbstractTreeSpliterator<T, Binary<T>> {
    private static final int BASE_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;

    public BinaryTreeSpliterator(Binary<T> tree) {
        this(initDeque(tree));
    }

    private BinaryTreeSpliterator(ArrayDeque<Binary<T>> deque) {
        super(deque, BASE_CHARACTERISTICS);
    }

    @Override
    protected T getValue(Binary<T> leaf) {
        return ((Trees.Leaf<T>) leaf).value();
    }

    @Override
    protected void traverseBranch(ArrayDeque<Binary<T>> queue, Binary<T> branch) {
        Binary.Branch<T> b = (Binary.Branch<T>) branch;
        if (b.right() != null) {
            queue.push(b.right());
        }
        if (b.left() != null) {
            queue.push(b.left());
        }
    }

    @Override
    protected AbstractTreeSpliterator<T, Binary<T>> createChild(ArrayDeque<Binary<T>> childDeque) {
        return new BinaryTreeSpliterator<>(childDeque);
    }
}
