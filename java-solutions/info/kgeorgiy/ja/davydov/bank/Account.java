package info.kgeorgiy.ja.davydov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface representing a bank account.
 */
public interface Account extends Remote {

    /**
     * Returns account identifier.
     *
     * @return account id
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money in the account.
     *
     * @return balance
     */
    int getAmount() throws RemoteException;

    /**
     * Atomically changes the balance by {@code delta}.
     *
     * @param delta account changes
     * @throws OutOfRangeException if the resulting balance would be negative or
     *                             would overflow {@code int}
     */
    void changeAmount(int delta) throws OutOfRangeException, RemoteException;
}
