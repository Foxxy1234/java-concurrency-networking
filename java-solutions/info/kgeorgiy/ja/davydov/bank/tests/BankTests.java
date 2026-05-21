package info.kgeorgiy.ja.davydov.bank.tests;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;

import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * JUnit Platform Launcher entry point for the bank test suite.
 */
public final class BankTests {

    private BankTests() {}

    public static void main(final String[] args) {
        final LauncherDiscoveryRequest tests = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("info.kgeorgiy.ja.davydov.bank.tests"))
                .build();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(tests, listener);
        final TestExecutionSummary result = listener.getSummary();
        result.printFailuresTo(new PrintWriter(System.err, true));
        final int rc = result.getTotalFailureCount() > 0 ? 1 : 0;
        System.out.println(rc);
        System.exit(rc);
    }
}
