package info.kgeorgiy.ja.davydov.bank.local;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.Person;

/**
 * Serializable local snapshot of a {@link Person} and all their accounts.
 */
public class LocalPerson implements Person, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String firstName;
    private final String lastName;
    private final String passport;
    private final HashMap<String, Account> accounts;

    /**
     * Creates a snapshot with the given personal data and accounts.
     *
     * @param firstName first name
     * @param lastName  last name
     * @param passport  passport number
     * @param accounts  map from subId to {@link LocalAccount} snapshot
     */
    public LocalPerson(final String firstName, final String lastName, final String passport,
                       final Map<String, LocalAccount> accounts) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
        this.accounts = new HashMap<>(accounts);
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
    public Account getAccount(final String subId) {
        return accounts.get(subId);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Account> getAccounts() {
        return Map.copyOf(accounts);
    }

    /**
     * Creates a local-only account if one with the given subId does not already exist.
     *
     * @param subId account sub-identifier
     * @return existing or newly created local account
     */
    public LocalAccount createAccount(final String subId) {
        return (LocalAccount) accounts.computeIfAbsent(subId, k -> new LocalAccount(passport + ":" + k, 0));
    }
}
