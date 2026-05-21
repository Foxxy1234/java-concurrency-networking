package info.kgeorgiy.ja.davydov.bank.tests;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import info.kgeorgiy.ja.davydov.bank.Bank;
import info.kgeorgiy.ja.davydov.bank.remote.RemoteBank;

/**
 * Base for all bank test
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class AbstractBankTest {

    private final int port = 7777 + Math.floorMod(getClass().getSimpleName().hashCode(), 1000);
    private final String url = "//localhost:" + port + "/bank-test";

    private Registry registry;
    private RemoteBank bankImpl;

    protected Bank bank;

    @BeforeAll
    final void setUpRegistry() throws Exception {
        registry = LocateRegistry.createRegistry(port);
        bankImpl = new RemoteBank();
        Naming.rebind(url, bankImpl);
        bank = (Bank) Naming.lookup(url);
    }

    @AfterAll
    final void tearDownRegistry() throws Exception {
        Naming.unbind(url);
        UnicastRemoteObject.unexportObject(bankImpl, true);
        UnicastRemoteObject.unexportObject(registry, true);
    }

    @BeforeEach
    final void resetBank() throws Exception {
        final RemoteBank nw = new RemoteBank();
        Naming.rebind(url, nw);
        final RemoteBank old = bankImpl;
        bankImpl = nw;
        bank = (Bank) Naming.lookup(url);
        UnicastRemoteObject.unexportObject(old, true);
    }
}
