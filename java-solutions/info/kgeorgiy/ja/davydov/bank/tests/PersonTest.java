package info.kgeorgiy.ja.davydov.bank.tests;

import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextPassport;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomFirstName;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomLastName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.Person;

@TestInstance(Lifecycle.PER_CLASS)
class PersonTest extends AbstractBankTest {

    @Test
    void create() throws Exception {
        final String first = randomFirstName();
        final String last = randomLastName();
        final String passport = nextPassport();
        final Person p = bank.createPerson(first, last, passport);
        assertNotNull(p);
        assertEquals(first, p.getFirstName());
        assertEquals(last, p.getLastName());
        assertEquals(passport, p.getPassport());
    }

    @Test
    void getUnknown() throws Exception {
        assertNull(bank.getPerson("null-" + nextPassport()));
    }

    @Test
    void createTwice() throws Exception {
        final String first = randomFirstName();
        final String last = randomLastName();
        final String passport = nextPassport();
        bank.createPerson(first, last, passport);
        final Person p = bank.createPerson(first, last, passport);
        assertEquals(first, p.getFirstName());
        assertEquals(last, p.getLastName());
    }

    @Test
    void getAccounts() throws Exception {
        final String passport = nextPassport();
        final Person p = bank.createPerson(randomFirstName(), randomLastName(), passport);
        bank.createAccount(passport + ":a");
        bank.createAccount(passport + ":b");
        final Map<String, Account> accs = p.getAccounts();
        assertEquals(2, accs.size());
        assertTrue(accs.containsKey("a"));
        assertTrue(accs.containsKey("b"));
    }

    @Test
    void accountIsCorrect() throws Exception {
        final String passport = nextPassport();
        final Person p = bank.createPerson(randomFirstName(), randomLastName(), passport);
        bank.createAccount(passport + ":acc");
        p.getAccount("acc").changeAmount(150);
        assertEquals(150, bank.getAccount(passport + ":acc").getAmount());
    }

    @Test
    void getUnknownAccount() throws Exception {
        final Person p = bank.createPerson(randomFirstName(), randomLastName(), nextPassport());
        assertNull(p.getAccount("nope"));
    }
}
