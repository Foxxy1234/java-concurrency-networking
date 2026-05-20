package info.kgeorgiy.ja.davydov.bank;

import info.kgeorgiy.ja.davydov.base.AbstractMain;
import info.kgeorgiy.ja.davydov.bank.remote.RemoteBank;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;

/**
 * Starts the bank RMI server and registers it in the local RMI registry.
 */
public class Server {

    static final int DEFAULT_PORT = 8888;

    private Server() {
    }

    private static final class Main extends AbstractMain {
        private static final String USAGE = "Usage: Server [port]";

        @Override
        protected void run(final String[] args) throws Exception {
            final int port = parseOptionalInt(args, 0, DEFAULT_PORT, "port");
            LocateRegistry.createRegistry(port);
            final RemoteBank bank = new RemoteBank();
            Naming.rebind("//localhost:" + port + "/bank", bank);
            System.out.println("Bank server started on port " + port);
        }

        @Override
        protected String usage() {
            return USAGE;
        }

        @Override
        protected int maxArgs() {
            return 1;
        }

        @Override
        protected void handleException(final Exception e) {
            if (e instanceof RemoteException) {
                System.err.println("Failed to start bank server: " + e.getMessage());
            } else if (e instanceof MalformedURLException) {
                System.err.println("Malformed URL: " + e.getMessage());
            } else {
                super.handleException(e);
            }
        }
    }

    /**
     * Entry point. Starts the RMI registry on the given (or default) port and binds
     * a fresh {@link RemoteBank} under the name {@code "bank"}.
     *
     * @param args optional single argument: port number
     */
    public static void main(final String[] args) {
        new Main().main(args);
    }
}
