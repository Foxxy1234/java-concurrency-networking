package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.java.advanced.streams.Trees.Nary;

public class NaryTreeSpliterator<T> extends AbstractTreeSpliterator<T, Nary<T>> {
    private static final int BASE_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;

    public NaryTreeSpliterator(Nary<T> tree) {
        this(initDeque(tree));
    }

    private NaryTreeSpliterator(ArrayDeque<Nary<T>> deque) {
        super(deque, BASE_CHARACTERISTICS);
    }

    @Override
    protected T getValue(Nary<T> leaf) {
        return ((Trees.Leaf<T>) leaf).value();
    }

    @Override
    protected void traverseBranch(ArrayDeque<Nary<T>> queue, Nary<T> branch) {
        List<? extends Nary<T>> children = ((Nary.Node<T>) branch).children();
        for (int i = children.size() - 1; i >= 0; i--) {
            queue.push(children.get(i));
        }
    }

    @Override
    protected AbstractTreeSpliterator<T, Nary<T>> createChild(ArrayDeque<Nary<T>> childDeque) {
        return new NaryTreeSpliterator<>(childDeque);
    }
}
