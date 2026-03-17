package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.java.advanced.streams.Trees.Binary;
import info.kgeorgiy.java.advanced.streams.Trees.Nary;

public class TreeSpliterator<T, N> extends AbstractTreeSpliterator<T, N> {
    private static final int BASE_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE;

    private final Function<? super N, ? extends T> valueExtractor;
    private final BiConsumer<ArrayDeque<N>, N> branchTraverser;

    private TreeSpliterator(ArrayDeque<N> deque, Function<? super N, ? extends T> valueExtractor,
            BiConsumer<ArrayDeque<N>, N> branchTraverser) {
        super(deque, BASE_CHARACTERISTICS);
        this.valueExtractor = valueExtractor;
        this.branchTraverser = branchTraverser;
    }

    public static <T> TreeSpliterator<T, Binary<T>> binary(Binary<T> tree) {
        return new TreeSpliterator<>(initDeque(tree), leaf -> ((Trees.Leaf<T>) leaf).value(), (queue, branch) -> {
            Binary.Branch<T> b = (Binary.Branch<T>) branch;
            if (b.right() != null) {
                queue.push(b.right());
            }
            if (b.left() != null) {
                queue.push(b.left());
            }
        });
    }

    public static <T> TreeSpliterator<T, Nary<T>> nary(Nary<T> tree) {
        return new TreeSpliterator<>(initDeque(tree), leaf -> ((Trees.Leaf<T>) leaf).value(), (queue, branch) -> {
            List<? extends Nary<T>> children = ((Nary.Node<T>) branch).children();
            for (int i = children.size() - 1; i >= 0; i--) {
                queue.push(children.get(i));
            }
        });
    }

    @Override
    protected T getValue(N leaf) {
        return valueExtractor.apply(leaf);
    }

    @Override
    protected void traverseBranch(ArrayDeque<N> queue, N branch) {
        branchTraverser.accept(queue, branch);
    }

    @Override
    protected AbstractTreeSpliterator<T, N> createChild(ArrayDeque<N> childQueue) {
        return new TreeSpliterator<>(childQueue, valueExtractor, branchTraverser);
    }
}
