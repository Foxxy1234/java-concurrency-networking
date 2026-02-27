package info.kgeorgiy.ja.Davydov.walk;

public interface HashFunction {
  void update(byte[] buf, int off, int len);

  void update(byte[] buf);

  void update(byte b);

  byte[] getHash();

  void reset();
}