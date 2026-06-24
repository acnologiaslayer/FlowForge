import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * A tiny standalone JUnit 5 runner used so the FlowForge test suite can be
 * executed from the command line without Maven or Gradle installed (only the
 * JUnit jars in ~/.m2 are required). It discovers every test under the
 * {@code com.flowforge} package, runs them and prints a summary, exiting with
 * a non-zero status if any test fails.
 */
public class JUnitRunner {

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = request()
                .selectors(selectPackage("com.flowforge"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out);
        summary.printTo(out);
        summary.printFailuresTo(out);
        out.flush();

        System.out.printf("Tests run: %d, passed: %d, failed: %d, skipped: %d%n",
                summary.getTestsStartedCount(),
                summary.getTestsSucceededCount(),
                summary.getTestsFailedCount(),
                summary.getTestsSkippedCount());

        System.exit(summary.getTestsFailedCount() == 0 ? 0 : 1);
    }
}
