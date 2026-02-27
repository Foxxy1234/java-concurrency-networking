package info.kgeorgiy.ja.Davydov.walk;

public class FNVHashFunction implements PrefixHashFunction {

	private static final int FNV_START = 0x811c9dc5;
	private static final int FNV_PRIME = 0x01000193;

	private int hash = FNV_START;

	@Override
	public void update(byte b) {
		hash *= FNV_PRIME;
		hash ^= (b & 0xFF);
	}

	@Override
	public void update(byte[] buf, int off, int len) {
		for (int i = off; i < off + len; i++) {
			update(buf[i]);
		}
	}

	@Override
	public void update(byte[] buf) {
		update(buf, 0, buf.length);
	}

	@Override
	public byte[] getHash() {
		return new byte[] {
				(byte) (hash >>> 24),
				(byte) (hash >>> 16),
				(byte) (hash >>> 8),
				(byte) (hash)
		};
	}

	public void reset() {
		hash = FNV_START;
	}

}