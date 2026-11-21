package online.foxxy239.concurrentnetworking.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteBank extends UnicastRemoteObject implements AutoCloseable {
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    public RemoteBank() throws RemoteException {
        super();
    }

    public void createAccount(final String id) {
        accounts.putIfAbsent(id, new Account());
    }

    public int amount(final String id) {
        return account(id).amount();
    }

    public void changeAmount(final String id, final int delta) {
        account(id).change(delta);
    }

    private Account account(final String id) {
        final Account account = accounts.get(id);
        if (account == null) {
            throw new IllegalArgumentException("Unknown account: " + id);
        }
        return account;
    }

    @Override
    public void close() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (final java.rmi.NoSuchObjectException ignored) {
            // The object may already have been unexported.
        }
    }

    private static final class Account {
        private int amount;

        synchronized int amount() {
            return amount;
        }

        synchronized void change(final int delta) {
            final long next = (long) amount + delta;
            if (next < 0 || next > Integer.MAX_VALUE) {
                return;
            }
            amount = (int) next;
        }
    }
}
