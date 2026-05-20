package info.kgeorgiy.ja.davydov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import info.kgeorgiy.ja.davydov.base.AbstractMain;

/**
 * Command-line bank client.
 */
public class Client {

    private Client() {
    }

    private static final class Main extends AbstractMain {
        private static final String USAGE = "Usage: Client firstName lastName passport subId delta";

        @Override
        protected void run(final String[] args) throws Exception {
            final String firstName = args[0];
            final String lastName = args[1];
            final String passport = args[2];
            final String subId = args[3];
            final int delta = parseRequiredInt(args, 4, "delta");

            final Bank bank = (Bank) Naming.lookup("//localhost:" + Server.DEFAULT_PORT + "/bank");

            final boolean personExisted = bank.getPerson(passport) != null;
            bank.createPerson(firstName, lastName, passport);
            System.out.println((personExisted ? "Found existing person: " : "Created person: ")
                    + firstName + " " + lastName);
            final String accountId = passport + ":" + subId;
            final boolean accountExisted = bank.getAccount(accountId) != null;
            final Account account = bank.createAccount(accountId);
            if (!accountExisted) {
                System.out.println("Created account: " + accountId);
            }
            try {
                account.changeAmount(delta);
                System.out.println("Balance of " + accountId + ": " + account.getAmount());
            } catch (final OutOfRangeException e) {
                System.err.println(e.getMessage());
            }
        }

        @Override
        protected String usage() {
            return USAGE;
        }

        @Override
        protected int minArgs() {
            return 5;
        }

        @Override
        protected int maxArgs() {
            return 5;
        }

        @Override
        protected void handleException(final Exception e) {
            if (e instanceof NotBoundException) {
                System.err.println("Bank not found");
            } else if (e instanceof MalformedURLException) {
                System.err.println("Malformed URL: " + e.getMessage());
            } else if (e instanceof RemoteException) {
                System.err.println("Remote error: " + e.getMessage());
            } else {
                super.handleException(e);
            }
        }
    }

    /**
     * Entry point.
     *
     * @param args {@code firstName lastName passport subId delta}
     */
    public static void main(final String[] args) {
        new Main().main(args);
    }
}
