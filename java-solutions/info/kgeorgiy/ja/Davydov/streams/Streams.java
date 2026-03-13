package info.kgeorgiy.ja.Davydov.streams;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import info.kgeorgiy.java.advanced.streams.*;
import info.kgeorgiy.java.advanced.streams.Trees.*;

public class Streams implements HardStreams {
    private static class Ref<T> {
        T value;
        boolean hasValue;

        Ref(T value) {
            this.value = value;
        }

        Ref() {
            this(null);
        }
    }

    private static class NormalLinkedList<T> {
        private Node<T> head, tail;
        private int size;

        static class Node<T> {
            T value;
            Node<T> next;

            Node(T value) {
                this.value = value;
            }
        }

        void add(T value) {
            Node<T> node = new Node<>(value);
            if (head == null)
                head = node;
            else
                tail.next = node;
            tail = node;
            size++;
        }

        T get(int num) {
            Node<T> res = head;
            while (num-- != 0)
                res = res.next;
            return res.value;
        }

        NormalLinkedList<T> merge(NormalLinkedList<T> other) {
            if (other.head == null)
                return this;
            if (head == null)
                return other;
            tail.next = other.head;
            tail = other.tail;
            size += other.size;
            return this;
        }
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return Collector.of(Ref<T>::new, (ref, nw) -> {
            if (!ref.hasValue) {
                ref.value = nw;
                ref.hasValue = true;
            }
        }, (a, b) -> a.hasValue ? a : b, ref -> ref.hasValue ? Optional.ofNullable(ref.value) : Optional.empty(),
                Collector.Characteristics.CONCURRENT);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return Collector.of(Ref<T>::new, (ref, nw) -> {
            ref.value = nw;
            ref.hasValue = true;
        }, (a, b) -> b.hasValue ? b : a, ref -> ref.hasValue ? Optional.ofNullable(ref.value) : Optional.empty(),
                Collector.Characteristics.CONCURRENT);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return Collector.of(NormalLinkedList<T>::new, NormalLinkedList<T>::add, NormalLinkedList<T>::merge,
                list -> list.size == 0 ? Optional.empty() : Optional.ofNullable(list.get(list.size / 2)),
                Collector.Characteristics.CONCURRENT);
    }

    private int commonPrefixSize(CharSequence a, CharSequence b) {
        int i = 0;
        while (i < a.length() && i < b.length() && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return Collector.of(() -> new Ref<>(new StringBuilder()), (acc, nw) -> {
            if (!acc.hasValue) {
                acc.value.append(nw);
                acc.hasValue = true;
            } else
                acc.value.delete(commonPrefixSize(acc.value, nw), acc.value.length());
        }, (a, b) -> {
            if (!a.hasValue) {
                return b;
            }
            if (!b.hasValue) {
                return a;
            }
            a.value.delete(commonPrefixSize(a.value, b.value), a.value.length());
            return a;
        }, acc -> acc.hasValue ? acc.value.toString() : "", Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.UNORDERED);
    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return Collectors.mapping(cs -> (CharSequence) new StringBuilder(cs).reverse(),
                Collectors.collectingAndThen(commonPrefix(), s -> new StringBuilder(s).reverse().toString()));
    }

    @Override
    public <T> Collector<T, ?, List<T>> head(int k) {
        return Collector.of(ArrayList<T>::new, (arr, nw) -> {
            if (arr.size() < k) {
                arr.add(nw);
            }
        }, (a, b) -> {
            throw new IllegalStateException("Parallel not supported");
        }, list -> list);
    }

    @Override
    public <T> Collector<T, ?, List<T>> tail(int k) {
        if (k <= 0) {
            return Collector.of(ArrayList<T>::new, (a, nw) -> {
            }, (a, b) -> a, a -> a);
        }
        return Collector.of(ArrayDeque<T>::new, (arr, nw) -> {
            if (arr.size() == k) {
                arr.removeFirst();
            }
            arr.addLast(nw);
        }, (a, b) -> {
            throw new IllegalStateException("Parallel not supported");
        }, deq -> new ArrayList<>(deq));
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> kth(int k) {
        class IndexedRef {
            T value;
            int index = 0;
        }
        return Collector.of(IndexedRef::new, (cb, nw) -> {
            if (cb.index++ == k) {
                cb.value = nw;
            }
        }, (a, b) -> {
            throw new IllegalStateException("Parallel not supported");
        }, cb -> cb.index > k ? Optional.ofNullable(cb.value) : Optional.empty());
    }

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Binary<T> tree) {
        return new BinaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(SizedBinary<T> tree) {
        return new SizedBinaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Nary<T> tree) {
        return new NaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Spliterator<T> nestedBinaryTreeSpliterator(Binary<List<T>> tree) {
        return new NestedTreeSpliterator<>(binaryTreeSpliterator(tree));
    }

    @Override
    public <T> Spliterator<T> nestedSizedBinaryTreeSpliterator(SizedBinary<List<T>> tree) {
        return new NestedTreeSpliterator<>(sizedBinaryTreeSpliterator(tree));
    }

    @Override
    public <T> Spliterator<T> nestedNaryTreeSpliterator(Nary<List<T>> tree) {
        return new NestedTreeSpliterator<>(naryTreeSpliterator(tree));
    }

    private Gatherer<CharSequence, ?, CharSequence> stringSubsequences(
            BiFunction<CharSequence, Integer, CharSequence> getSubstr) {
        return Gatherer.ofSequential(() -> null, Gatherer.Integrator.ofGreedy((_, str, ds) -> {
            for (int i = 1; i <= str.length(); i++) {
                if (!ds.push(getSubstr.apply(str, i))) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringPrefixes() {
        return stringSubsequences((str, n) -> str.subSequence(0, n));
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringSuffixes() {
        return stringSubsequences((str, n) -> str.subSequence(str.length() - n, str.length()));
    }

    private <T> Gatherer<T, ?, T> filterByIndex(Predicate<Integer> p) {
        class Index {
            int value = 0;
        }
        return Gatherer.ofSequential(Index::new, Gatherer.Integrator.ofGreedy((index, el, ds) -> {
            if (p.test(++index.value)) {
                ds.push(el);
            }
            return true;
        }));
    }

    @Override
    public <T> Gatherer<T, ?, T> nth(int n) {
        return filterByIndex(i -> i % n == 0);
    }

    @Override
    public <T> Gatherer<T, ?, T> ithOfN(int i, int n) {
        return filterByIndex(ind -> (ind - 1) % n == i);
    }

    @Override
    public <T> Gatherer<T, ?, T> distinctPrefix() {
        return distinctPrefixBy(Function.identity());
    }

    @Override
    public <T, K> Gatherer<T, ?, T> distinctPrefixBy(Function<? super T, K> keyF) {
        return Gatherer.ofSequential(HashSet::new,
                Gatherer.Integrator.of((prev, nw, ds) -> prev.add(keyF.apply(nw)) && ds.push(nw)));
    }
}
