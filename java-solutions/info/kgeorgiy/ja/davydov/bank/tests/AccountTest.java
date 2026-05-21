package info.kgeorgiy.ja.davydov.bank.tests;

import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextAccountId;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextPassport;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomFirstName;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomLastName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.OutOfRangeException;

@TestInstance(Lifecycle.PER_CLASS)
class AccountTest extends AbstractBankTest {

    @Test
    void createAccount() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final String id = nextAccountId(passport);
        final Account a = bank.createAccount(id);
        assertNotNull(a);
        assertEquals(id, a.getId());
        assertEquals(0, a.getAmount());
    }

    @Test
    void getUnknown() throws Exception {
        assertNull(bank.getAccount("nobody:noacc"));
    }

    @Test
    void createTwice() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final String id = nextAccountId(passport);
        bank.createAccount(id);
        assertEquals(0, bank.createAccount(id).getAmount());
    }

    @Test
    void deposit() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account a = bank.createAccount(nextAccountId(passport));
        a.changeAmount(500);
        assertEquals(500, a.getAmount());
    }

    @Test
    void underflow() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account a = bank.createAccount(nextAccountId(passport));
        assertThrows(OutOfRangeException.class, () -> a.changeAmount(-1));
    }

    @Test
    void overflow() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account a = bank.createAccount(nextAccountId(passport));
        a.changeAmount(Integer.MAX_VALUE);
        assertThrows(OutOfRangeException.class, () -> a.changeAmount(1));
    }

    @Test
    void sequentialChanges() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account a = bank.createAccount(nextAccountId(passport));
        int expected = 0;
        int[] diffs = { 100, -67, 500, -42, -52};
        for (final int delta : diffs) {
            a.changeAmount(delta);
            expected += delta;
            assertEquals(expected, a.getAmount());
        }
    }

}
