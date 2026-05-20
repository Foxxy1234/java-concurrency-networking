package info.kgeorgiy.ja.davydov.bank.remote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.Person;

/**
 * Remote implementation of {@link Person}.
 */
@SuppressWarnings("serial")
public class RemotePerson extends UnicastRemoteObject implements Person {

    private final String firstName;
    private final String lastName;
    private final String passport;
    private final ConcurrentHashMap<String, RemoteAccount> accounts;

    /**
     * Creates a new remote person.
     *
     * @param firstName first name
     * @param lastName  last name
     * @param passport  unique passport number
     * @param accounts  shared bank accounts map
     */
    public RemotePerson(final String firstName, final String lastName, final String passport,
            final ConcurrentHashMap<String, RemoteAccount> accounts)
            throws RemoteException {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
        this.accounts = accounts;
    }

    /** {@inheritDoc} */
    @Override
    public String getFirstName() {
        return firstName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLastName() {
        return lastName;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassport() {
        return passport;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Account> getAccounts() throws RemoteException {
        final String prefix = passport + ":";
        return accounts.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(prefix.length()),
                        Map.Entry::getValue));
    }

    /** {@inheritDoc} */
    @Override
    public Account getAccount(final String subId) throws RemoteException {
        return accounts.get(passport + ":" + subId);
    }
}
