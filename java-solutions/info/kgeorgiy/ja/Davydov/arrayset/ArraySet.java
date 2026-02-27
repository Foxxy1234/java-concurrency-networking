package info.kgeorgiy.ja.Davydov.arrayset;

import java.util.*;

public class ArraySet<T> implements NavigableSet<T> {

  private final List<T> data;
  private final Comparator<? super T> comp;

  public ArraySet(Collection<? extends T> collection, Comparator<? super T> comp) {
    this.comp = comp;
    TreeSet<T> sorted = new TreeSet<>(comp);
    sorted.addAll(collection);
    this.data = Collections.unmodifiableList(new ArrayList<>(sorted));
  }

  private ArraySet(List<T> collection, Comparator<? super T> comp) {
    this.comp = comp;
    this.data = collection;
  }

  public ArraySet(Collection<? extends T> collection) {
    this(collection, null);
  }

  public ArraySet() {
    this(Collections.emptyList(), null);
  }

  private int FindElementHigher(T x) {
    if (x == null && comp == null) {
      throw new NullPointerException("Not supported null values in this set");
    }
    int pos = Collections.binarySearch(data, x, comp);
    return pos >= 0 ? pos + 1 : -pos - 1;
  }

  private int FindElementCeil(T x) {
    if (x == null && comp == null) {
      throw new NullPointerException("Not supported null values in this set");
    }
    int pos = Collections.binarySearch(data, x, comp);
    return pos >= 0 ? pos : -pos - 1;
  }

  private T getByIndex(int index) {
    if (index < 0 || index >= size()) {
      return null;
    }
    return data.get(index);
  }

  private int compare(T a, T b) {
    if (comp != null) {
      return comp.compare(a, b);
    }
    return ((Comparable<T>) a).compareTo(b);
  }

  @Override
  public Comparator<? super T> comparator() {
    return comp;
  }

  @Override
  public T first() {
    return data.getFirst();
  }

  @Override
  public T last() {
    return data.getLast();
  }

  @Override
  public int size() {
    return data.size();
  }

  @Override
  public T ceiling(T x) {
    return getByIndex(FindElementCeil(x));
  }

  @Override
  public Iterator<T> descendingIterator() {
    return data.reversed().iterator();
  }

  @Override
  public NavigableSet<T> descendingSet() {
    return new ArraySet<>(data.reversed(), Collections.reverseOrder(comp));
  }

  @Override
  public T floor(T x) {
    return getByIndex(FindElementHigher(x) - 1);
  }

  @Override
  public SortedSet<T> headSet(T x) {
    return headSet(x, false);
  }

  @Override
  public NavigableSet<T> headSet(T x, boolean b) {
    int end = b ? FindElementHigher(x) : FindElementCeil(x);
    return new ArraySet<>(data.subList(0, end), comp);
  }

  @Override
  public T higher(T x) {
    return getByIndex(FindElementHigher(x));
  }

  @Override
  public Iterator<T> iterator() {
    return data.iterator();
  }

  @Override
  public T lower(T x) {
    return getByIndex(FindElementCeil(x) - 1);
  }

  @Override
  public T pollFirst() {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public T pollLast() {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public SortedSet<T> subSet(T x, T y) {
    return subSet(x, true, y, false);
  }

  @Override
  public boolean contains(Object x) {
    if (x == null && comp == null) {
      throw new NullPointerException("Not supported null values in this set");
    }
    return Collections.binarySearch(data, (T) x, comp) >= 0;
  }

  @Override
  public NavigableSet<T> subSet(T x, boolean b1, T y, boolean b2) {
    if (compare(x, y) > 0) {
      throw new IllegalArgumentException("toKey > endKey");
    }
    int begin = b1 ? FindElementCeil(x) : FindElementHigher(x);
    int end = b2 ? FindElementHigher(y) : FindElementCeil(y);
    if (begin > end) {
      return new ArraySet<>(Collections.emptyList(), comp);
    }
    return new ArraySet<>(data.subList(begin, end), comp);
  }

  @Override
  public SortedSet<T> tailSet(T x) {
    return tailSet(x, true);
  }

  @Override
  public NavigableSet<T> tailSet(T x, boolean b) {
    int begin = b ? FindElementCeil(x) : FindElementHigher(x);
    return new ArraySet<>(data.subList(begin, data.size()), comp);
  }

  @Override
  public boolean add(T x) {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("Invalid method for an immutable set");
  }

  @Override
  public Object[] toArray() {
    return data.toArray();
  }

  @Override
  public <E> E[] toArray(E[] arr) {
    return data.toArray(arr);
  }

}
