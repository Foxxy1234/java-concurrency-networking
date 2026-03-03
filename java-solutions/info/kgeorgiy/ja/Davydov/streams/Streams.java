package info.kgeorgiy.ja.Davydov.streams;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Gatherer;

import info.kgeorgiy.java.advanced.streams.HardStreams;
import info.kgeorgiy.java.advanced.streams.Trees.Binary;
import info.kgeorgiy.java.advanced.streams.Trees.Nary;
import info.kgeorgiy.java.advanced.streams.Trees.SizedBinary;

public class Streams implements HardStreams {

  @Override
  public <T> Spliterator<T> binaryTreeSpliterator(Binary<T> tree) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'binaryTreeSpliterator'");
  }

  @Override
  public <T> Spliterator<T> sizedBinaryTreeSpliterator(SizedBinary<T> tree) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'sizedBinaryTreeSpliterator'");
  }

  @Override
  public <T> Spliterator<T> naryTreeSpliterator(Nary<T> tree) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'naryTreeSpliterator'");
  }

  @Override
  public <T> Collector<T, ?, Optional<T>> first() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'first'");
  }

  @Override
  public <T> Collector<T, ?, Optional<T>> last() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'last'");
  }

  @Override
  public <T> Collector<T, ?, Optional<T>> middle() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'middle'");
  }

  @Override
  public Collector<CharSequence, ?, String> commonPrefix() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'commonPrefix'");
  }

  @Override
  public Collector<CharSequence, ?, String> commonSuffix() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'commonSuffix'");
  }

  @Override
  public Gatherer<CharSequence, ?, CharSequence> stringPrefixes() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'stringPrefixes'");
  }

  @Override
  public <T> Gatherer<T, ?, T> nth(int n) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'nth'");
  }

  @Override
  public <T> Gatherer<T, ?, T> distinctPrefix() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'distinctPrefix'");
  }

  @Override
  public <T> Spliterator<T> nestedBinaryTreeSpliterator(Binary<List<T>> tree) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'nestedBinaryTreeSpliterator'");
  }

  @Override
  public <T> Spliterator<T> nestedSizedBinaryTreeSpliterator(SizedBinary<List<T>> tree) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'nestedSizedBinaryTreeSpliterator'");
  }

  @Override
  public <T> Spliterator<T> nestedNaryTreeSpliterator(Nary<List<T>> tree) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'nestedNaryTreeSpliterator'");
  }

  @Override
  public <T> Collector<T, ?, List<T>> head(int k) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'head'");
  }

  @Override
  public <T> Collector<T, ?, List<T>> tail(int k) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'tail'");
  }

  @Override
  public <T> Collector<T, ?, Optional<T>> kth(int k) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'kth'");
  }

  @Override
  public Gatherer<CharSequence, ?, CharSequence> stringSuffixes() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'stringSuffixes'");
  }

  @Override
  public <T> Gatherer<T, ?, T> ithOfN(int i, int n) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'ithOfN'");
  }

  @Override
  public <T, K> Gatherer<T, ?, T> distinctPrefixBy(Function<? super T, K> keyF) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'distinctPrefixBy'");
  }
  
}
