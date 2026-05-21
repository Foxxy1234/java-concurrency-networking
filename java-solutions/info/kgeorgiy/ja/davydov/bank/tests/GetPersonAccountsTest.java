package info.kgeorgiy.ja.davydov.bank.tests;

import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextPassport;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomFirstName;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomLastName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Account;

@TestInstance(Lifecycle.PER_CLASS)
class GetPersonAccountsTest extends AbstractBankTest {

    @Test
    void allAccounts() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        bank.createAccount(passport + ":a");
        bank.createAccount(passport + ":b");
        bank.createAccount(passport + ":c");
        final Map<String, Account> accs = bank.getPersonAccounts(passport);
        assertEquals(3, accs.size());
        assertTrue(accs.containsKey(passport + ":a"));
        assertTrue(accs.containsKey(passport + ":b"));
        assertTrue(accs.containsKey(passport + ":c"));
    }

    @Test
    void emptyForNoAccounts() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Map<String, Account> accs = bank.getPersonAccounts(passport);
        assertNotNull(accs);
        assertEquals(0, accs.size());
    }

    @Test
    void commonSubId() throws Exception {
        final String pa = nextPassport();
        final String pb = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), pa);
        bank.createPerson(randomFirstName(), randomLastName(), pb);
        final Account aAcc = bank.createAccount(pa + ":common");
        final Account bAcc = bank.createAccount(pb + ":common");
        aAcc.changeAmount(100);
        bAcc.changeAmount(200);
        assertEquals(100, bank.getAccount(pa + ":common").getAmount());
        assertEquals(200, bank.getAccount(pb + ":common").getAmount());
    }
}
