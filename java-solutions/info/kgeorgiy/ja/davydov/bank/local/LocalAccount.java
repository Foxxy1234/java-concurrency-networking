package info.kgeorgiy.ja.davydov.bank.local;

import java.io.Serial;
import java.io.Serializable;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.OutOfRangeException;

/**
 * Serializable local snapshot of an {@link Account}.
 */
public class LocalAccount implements Account, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private int amount;

    /**
     * Creates a snapshot with the given id and balance.
     *
     * @param id     account id
     * @param amount balance at snapshot time
     */
    public LocalAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized int getAmount() {
        return amount;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void changeAmount(final int delta) throws OutOfRangeException {
        final long result = (long) amount + (long) delta;
        if (result < 0 || result > Integer.MAX_VALUE) {
            throw new OutOfRangeException(id, amount, delta);
        }
        amount = (int) result;
    }
}
