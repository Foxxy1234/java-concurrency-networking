package info.kgeorgiy.ja.Davydov.streams;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
public class NestedTreeSpliterator<T> implements Spliterator<T> {
    private final Spliterator<List<T>> source;
    private final int baseCharacteristics;
    private List<T> currentList = null;
    private int currentIndex = 0;
    private int currentEnd = 0;

    public NestedTreeSpliterator(Spliterator<List<T>> source) {
        this(source, source.characteristics() & Spliterator.ORDERED, null, 0, 0);
    }

    private NestedTreeSpliterator(Spliterator<List<T>> source, int baseCharacteristics, List<T> currentList,
            int currentIndex, int currentEnd) {
        this.source = source;
        this.baseCharacteristics = baseCharacteristics;
        this.currentList = currentList;
        this.currentIndex = currentIndex;
        this.currentEnd = currentEnd;
    }

    private boolean loadNextList() {
        currentList = null;
        currentIndex = 0;
        currentEnd = 0;
        while (source.tryAdvance(list -> {
            if (list != null && !list.isEmpty()) {
                currentList = list;
                currentEnd = list.size();
            }
        })) {
            if (currentList != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        while (true) {
            if (currentList != null && currentIndex < currentEnd) {
                action.accept(currentList.get(currentIndex++));
                return true;
            }
            if (!loadNextList()) {
                return false;
            }
        }
    }

    private Spliterator<T> trySplitWhenSizeOne() {
        List<T> copyList = currentList;
        int copyIndex = currentIndex;
        if (!loadNextList()) {
            return null;
        } else {
            return new NestedTreeSpliterator<>(Spliterators.emptySpliterator(), baseCharacteristics, copyList,
                    copyIndex, copyIndex + 1);
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        if (currentList != null && currentIndex < currentEnd) {
            int remaining = currentEnd - currentIndex;
            if (remaining <= 1) {
                return trySplitWhenSizeOne();
            }
            int middle = currentIndex + remaining / 2;
            NestedTreeSpliterator<T> child = new NestedTreeSpliterator<>(Spliterators.emptySpliterator(),
                    baseCharacteristics, currentList, currentIndex, middle);
            currentIndex = middle;
            return child;
        }

        Spliterator<List<T>> prefix = source.trySplit();
        if (prefix != null) {
            return new NestedTreeSpliterator<>(prefix, baseCharacteristics, null, 0, 0);
        }
        if (!loadNextList()) {
            return null;
        }
        int remaining = currentEnd - currentIndex;
        if (remaining <= 1) {
            return trySplitWhenSizeOne();
        }
        int middle = currentIndex + remaining / 2;
        NestedTreeSpliterator<T> child = new NestedTreeSpliterator<>(Spliterators.emptySpliterator(),
                baseCharacteristics, currentList, currentIndex, middle);
        currentIndex = middle;
        return child;
    }

    @Override
    public long estimateSize() {
        long sourceSize = source.estimateSize();
        long currentSize = currentEnd - currentIndex;
        if (sourceSize == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return sourceSize + currentSize;
    }

    @Override
    public int characteristics() {
        return baseCharacteristics;
    }
}
