package info.kgeorgiy.ja.davydov.bank.tests;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared helpers for bank tests
 */
final class BankTestHelper {

    private static final String[] FIRST_NAMES = {
            "Ivan", "Georgiy", "Andrew", "Dmitry", "Sergey"
    };

    private static final String[] LAST_NAMES = {
            "Stankevich", "Davydov", "Korneev", "Ivanov", "Petrov", "Sidorov"
    };

    private static final AtomicLong PASSPORT_SEQ = new AtomicLong();

    private static final AtomicLong ACCOUNT_SEQ = new AtomicLong();

    private BankTestHelper() {}

    /** Returns a random first name from the predefined pool. */
    public static String randomFirstName() {
        return FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
    }

    /** Returns a random last name from the predefined pool. */
    public static String randomLastName() {
        return LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
    }

    /** Returns a fresh unique passport id, */
    public static String nextPassport() {
        return "p-" + PASSPORT_SEQ.incrementAndGet();
    }

    /** Returns a fresh unique account id for the given passport. */
    public static String nextAccountId(final String passport) {
        return passport + ":acc-" + ACCOUNT_SEQ.incrementAndGet();
    }
}
