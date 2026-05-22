package info.kgeorgiy.ja.davydov.bank.remote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.Bank;
import info.kgeorgiy.ja.davydov.bank.Person;
import info.kgeorgiy.ja.davydov.bank.local.LocalAccount;
import info.kgeorgiy.ja.davydov.bank.local.LocalPerson;

/**
 * Remote implementation of {@link Bank}.
 */
@SuppressWarnings("serial")
public class RemoteBank extends UnicastRemoteObject implements Bank {

    private final ConcurrentHashMap<String, RemotePerson> persons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RemoteAccount> accounts = new ConcurrentHashMap<>();

    /**
     * Creates and exports a new bank instance on an anonymous port.
     *
     * @throws RemoteException on RMI export error
     */
    public RemoteBank() throws RemoteException {
    }

    /** {@inheritDoc} */
    @Override
    public Person createPerson(final String firstName, final String lastName, final String passport)
            throws RemoteException {
        final RemotePerson nw = new RemotePerson(firstName, lastName, passport, accounts);
        final RemotePerson existing = persons.putIfAbsent(passport, nw);
        // :NOTE: кидать исключение, если человек уже есть
        if (existing != null) {
            UnicastRemoteObject.unexportObject(nw, true);
            if (!existing.getFirstName().equals(firstName) || !existing.getLastName().equals(lastName)) {
                throw new IllegalStateException("Person data mismatch for passport " + passport);
                // :NOTE: проверяемое исключние
            }
            return existing;
        }
        return nw;
    }

    /** {@inheritDoc} */
    @Override
    public Person getPerson(final String passport) {
        return persons.get(passport);
    }

    /** {@inheritDoc} */
    @Override
    public LocalPerson getLocalPerson(final String passport) throws RemoteException {
        final RemotePerson person = persons.get(passport);
        if (person == null)
            return null;

        final String prefix = passport + ":";
        final Map<String, LocalAccount> localAccounts = accounts.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(prefix.length()),
                        e -> new LocalAccount(e.getKey(), e.getValue().getAmount())));

        return new LocalPerson(
                person.getFirstName(),
                person.getLastName(),
                passport,
                localAccounts);
    }

    /** {@inheritDoc} */
    @Override
    public Account createAccount(final String id) throws RemoteException {
        final RemoteAccount created = new RemoteAccount(id);
        final RemoteAccount existing = accounts.putIfAbsent(id, created);
        if (existing != null) {
            UnicastRemoteObject.unexportObject(created, true);
            return existing;
        }
        return created;
    }

    /** {@inheritDoc} */
    @Override
    public Account getAccount(final String id) {
        return accounts.get(id);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Account> getPersonAccounts(final String passport) {
        final String prefix = passport + ":";
        // :NOTE: очень долго
        return accounts.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
