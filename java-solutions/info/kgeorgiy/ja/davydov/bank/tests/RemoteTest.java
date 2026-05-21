package info.kgeorgiy.ja.davydov.bank.tests;

import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextPassport;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomFirstName;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomLastName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Account;

@TestInstance(Lifecycle.PER_CLASS)
class RemoteTest extends AbstractBankTest {

    @Test
    void sameAccountFromTwoRefs() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account ref1 = bank.createAccount(passport + ":acc");
        final Account ref2 = bank.getAccount(passport + ":acc");
        ref1.changeAmount(250);
        assertEquals(250, ref2.getAmount());
        ref2.changeAmount(-199);
        assertEquals(51, ref1.getAmount());
    }

    @Test
    void accountsFromMapAreLive() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        bank.createAccount(passport + ":acc");
        final Account direct = bank.getAccount(passport + ":acc");
        direct.changeAmount(993);
        final Account fromMap = bank.getPersonAccounts(passport).get(passport + ":acc");
        assertEquals(993, fromMap.getAmount());
    }
}
