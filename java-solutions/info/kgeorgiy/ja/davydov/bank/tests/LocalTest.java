package info.kgeorgiy.ja.davydov.bank.tests;

import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextPassport;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomFirstName;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomLastName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.local.LocalPerson;

@TestInstance(Lifecycle.PER_CLASS)
class LocalTest extends AbstractBankTest {

    @Test
    void snapshotIsolatedFromRemote() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account remote = bank.createAccount(passport + ":acc");
        remote.changeAmount(100);
        final LocalPerson snapshot = bank.getLocalPerson(passport);
        assertNotNull(snapshot);
        assertEquals(100, snapshot.getAccount("acc").getAmount());
        remote.changeAmount(52);
        assertEquals(100, snapshot.getAccount("acc").getAmount());
    }

    @Test
    void localWritesDontRemote() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account remote = bank.createAccount(passport + ":acc");
        remote.changeAmount(50);
        final LocalPerson snapshot = bank.getLocalPerson(passport);
        assertNotNull(snapshot);
        snapshot.getAccount("acc").changeAmount(9000);
        assertEquals(50, bank.getAccount(passport + ":acc").getAmount());
    }

    @Test
    void getUnknown() throws Exception {
        assertNull(bank.getLocalPerson("null-" + nextPassport()));
    }

    @Test
    void personalData() throws Exception {
        final String first = randomFirstName();
        final String last = randomLastName();
        final String passport = nextPassport();
        bank.createPerson(first, last, passport);
        final LocalPerson lp = bank.getLocalPerson(passport);
        assertNotNull(lp);
        assertEquals(first, lp.getFirstName());
        assertEquals(last, lp.getLastName());
        assertEquals(passport, lp.getPassport());
    }

    @Test
    void snapshotFrozenAtTime() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        bank.createAccount(passport + ":first");
        final LocalPerson snapshot = bank.getLocalPerson(passport);
        bank.createAccount(passport + ":second");
        assertNotNull(snapshot.getAccount("first"));
        assertNull(snapshot.getAccount("second"));
    }

}
