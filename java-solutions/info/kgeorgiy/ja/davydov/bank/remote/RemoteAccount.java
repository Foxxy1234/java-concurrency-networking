package info.kgeorgiy.ja.davydov.bank.remote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.OutOfRangeException;

/**
 * Remote implementation of {@link Account}.
 */
@SuppressWarnings("serial")
public class RemoteAccount extends UnicastRemoteObject implements Account {
    private final String id;
    private int amount;

    /**
     * Creates a new account with zero initial balance.
     *
     * @param id full account id in the format {@code "passport:subId"}
     * @throws RemoteException on RMI export error
     */
    public RemoteAccount(final String id) throws RemoteException {
        this.id = id;
        this.amount = 0;
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
