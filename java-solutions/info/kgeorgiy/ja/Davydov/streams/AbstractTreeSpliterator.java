package info.kgeorgiy.ja.Davydov.streams;

import java.util.ArrayDeque;
import java.util.Spliterator;
import java.util.function.Consumer;

import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.java.advanced.streams.Trees.Binary;

public abstract class AbstractTreeSpliterator<T, N> implements Spliterator<T> {
    protected static boolean isLeaf(Object node) {
        return node instanceof Trees.Leaf;
    }

    private boolean knownSelfSize() {
        return (deque.size() == 1 && isLeaf(deque.getFirst()));
    }

    protected ArrayDeque<N> deque;
    private final int baseCharacteristic;

    protected AbstractTreeSpliterator(ArrayDeque<N> deque, int baseCharacteristic) {
        this.deque = deque;
        this.baseCharacteristic = baseCharacteristic;
    }

    protected abstract T getValue(N leaf);

    protected abstract void traverseBranch(ArrayDeque<N> queue, N branch);

    protected AbstractTreeSpliterator<T, N> createChild(ArrayDeque<N> childDeque) {
        throw new UnsupportedOperationException();
    }

    protected void afterLeafVisit() { // This function used in sized spliterators, but in common case they do nothing, so it is not abstract
    }

    protected static <T> ArrayDeque<T> initDeque(T tree) {
        ArrayDeque<T> deq = new ArrayDeque<>();
        if (tree != null) {
            deq.push(tree);
        }
        return deq;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        while (!deque.isEmpty()) {
            N node = deque.pop();
            if (isLeaf(node)) {
                afterLeafVisit();
                action.accept(getValue(node));
                return true;
            }
            traverseBranch(deque, node);
        }
        return false;
    }

    @Override
    public Spliterator<T> trySplit() {
        while (!deque.isEmpty()) {
            N first = deque.peek();
            if (isLeaf(first)) {
                if (deque.size() == 1) {
                    return null;
                }
                deque.pop();
                ArrayDeque<N> childQueue = new ArrayDeque<>();
                childQueue.push(first);
                return createChild(childQueue);
            }
            deque.pop();
            traverseBranch(deque, first);
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return knownSelfSize() ? 1 : Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        int ch = baseCharacteristic;
        if (knownSelfSize()) {
            ch |= Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        return ch;
    }
}
