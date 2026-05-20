package info.kgeorgiy.ja.davydov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Remote interface representing a bank customer identified by passport number.
 */
public interface Person extends Remote {

    /**
     * Returns the person's first name.
     *
     * @return first name
     */
    String getFirstName() throws RemoteException;

    /**
     * Returns the person's last name.
     *
     * @return last name
     */
    String getLastName() throws RemoteException;

    /**
     * Returns the passport number.
     *
     * @return passport number
     */
    String getPassport() throws RemoteException;

    /**
     * Returns all accounts belonging to this person.
     *
     * @return map from subId to account
     */
    Map<String, Account> getAccounts() throws RemoteException;

    /**
     * Returns the account with the given sub-identifier.
     *
     * @param subId account sub-identifier (the part after {@code ":"})
     * @return account, or {@code null} if not found
     */
    Account getAccount(String subId) throws RemoteException;
}
