package info.kgeorgiy.ja.davydov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import info.kgeorgiy.ja.davydov.bank.local.LocalPerson;

/**
 * Remote interface for the bank service.
 */
public interface Bank extends Remote {

    /**
     * Creates a new person or returns the existing one with the same passport.
     *
     * @param firstName first name
     * @param lastName  last name
     * @param passport  unique passport number
     * @return remote person reference
     */
    Person createPerson(String firstName, String lastName, String passport) throws RemoteException;

    /**
     * Looks up a person by passport number.
     *
     * @param passport passport number to look up
     * @return remote person reference, or {@code null} if not found
     */
    Person getPerson(String passport) throws RemoteException;

    /**
     * Returns a serialized local snapshot of the person and all their accounts.
     *
     * @param passport passport number to look up
     * @return serialized snapshot, or {@code null} if the person does not exist
     */
    LocalPerson getLocalPerson(String passport) throws RemoteException;

     /**
     * Creates a new account with specified identifier if it does not already exist.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

    /**
     * Returns all accounts belonging to the given person.
     *
     * @param passport passport number of the person
     * @return map from full account id to remote account reference and empty map if the person has no accounts or does not exist
     */
    Map<String, Account> getPersonAccounts(String passport) throws RemoteException;
}
