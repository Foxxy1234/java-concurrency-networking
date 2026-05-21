package info.kgeorgiy.ja.davydov.bank.tests;

import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextAccountId;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.nextPassport;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomFirstName;
import static info.kgeorgiy.ja.davydov.bank.tests.BankTestHelper.randomLastName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Account;
import info.kgeorgiy.ja.davydov.bank.OutOfRangeException;
import info.kgeorgiy.ja.davydov.bank.Person;
import info.kgeorgiy.ja.davydov.base.RequestExecutor;

@TestInstance(Lifecycle.PER_CLASS)
class ConcurrencyTest extends AbstractBankTest {

    @Test
    void accountCreation() throws Exception {
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        parallelRun(8, () -> bank.createAccount(passport + ":concurrent"));
        assertEquals(1, bank.getPersonAccounts(passport).size());
        assertEquals(0, bank.getAccount(passport + ":concurrent").getAmount());
    }

    @Test
    void personCreation() throws Exception {
        final String passport = nextPassport();
        final String first = randomFirstName();
        final String last = randomLastName();
        parallelRun(16, () -> bank.createPerson(first, last, passport));
        final Person p = bank.getPerson(passport);
        assertEquals(first, p.getFirstName());
        assertEquals(last, p.getLastName());
    }

    @Test
    void personConflictRace() throws Exception {
        final int workers = 64;
        final String passport = nextPassport();
        final AtomicInteger names = new AtomicInteger();
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        final List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        parallelRun(workers, () -> {
            final int n = names.getAndIncrement();
            try {
                bank.createPerson("First" + n, "Last" + n, passport);
                successes.incrementAndGet();
            } catch (final IllegalStateException e) {
                failed.incrementAndGet();
            } catch (final Throwable t) {
                unexpected.add(t);
            }
        });

        assertTrue(unexpected.isEmpty(), "unexpected errors: " + unexpected);
        assertEquals(1, successes.get(), "exactly one createPerson must been created");
        assertEquals(workers - 1, failed.get(), "all other createPerson calls must throw IllegalStateException");
    }

    @Test
    void decreaseRace() throws Exception {
        final int init = 80;
        final int workers = 100;
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account a = bank.createAccount(passport + ":wallet");
        a.changeAmount(init);

        final Result result = runRaces(workers, () -> a.changeAmount(-1));

        assertTrue(result.unexpected.isEmpty(), "unexpected errors: " + result.unexpected);
        assertEquals(init, result.successes.get(), "exactly `initial` withdrawals must succeed");
        assertEquals(workers - init, result.outOfRange.get(),
                "remaining decrease must each throw OutOfRangeException");
        assertEquals(0, a.getAmount(), "final balance must be zero");
    }

    @Test
    void increaseRace() throws Exception {
        final int remain = 13;
        final int workers = 100;
        final int init = Integer.MAX_VALUE - remain;
        final String passport = nextPassport();
        bank.createPerson(randomFirstName(), randomLastName(), passport);
        final Account a = bank.createAccount(nextAccountId(passport));
        a.changeAmount(init);

        final Result outcome = runRaces(workers, () -> a.changeAmount(1));
        assertTrue(outcome.unexpected.isEmpty(), "unexpected errors: " + outcome.unexpected);
        assertEquals(remain, outcome.successes.get(),
                "exactly remain increase must succeed");
        assertEquals(workers - remain, outcome.outOfRange.get(),
                "remaining increase must each throw OutOfRangeException");
        assertEquals(Integer.MAX_VALUE, a.getAmount(), "final balance must be Integer.MAX_VALUE");
    }
    @FunctionalInterface
    private interface Worker {
        void run() throws Exception;
    }

    private static final class Result {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger outOfRange = new AtomicInteger();
        final List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());
    }

    private static Result runRaces(final int threads, final Worker request) throws Exception {
        final Result out = new Result();
        parallelRun(threads, () -> {
            try {
                request.run();
                out.successes.incrementAndGet();
            } catch (final OutOfRangeException e) {
                out.outOfRange.incrementAndGet();
            } catch (final Throwable t) {
                out.unexpected.add(t);
            }
        });
        return out;
    }

    private static void parallelRun(final int threads, final Worker request) throws Exception {
        final CountDownLatch counterStart = new CountDownLatch(1);
        try (RequestExecutor executor = new RequestExecutor(threads)) {
            final Callable<Void> task = () -> {
                counterStart.await();
                request.run();
                return null;
            };
            final List<Future<Void>> results = IntStream.range(0, threads)
                    .mapToObj(i -> executor.submit(task))
                    .toList();
            counterStart.countDown();
            for (final Future<Void> f : results) {
                f.get(5, TimeUnit.SECONDS);
            }
        }
    }
}
