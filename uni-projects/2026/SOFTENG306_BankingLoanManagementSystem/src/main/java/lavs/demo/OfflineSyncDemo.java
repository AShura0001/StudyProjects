package lavs.demo;

import lavs.app.AppServices;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.LoanDetails;
import lavs.business.service.BasicLoanValidator;
import lavs.business.service.DraftSyncService.SyncResult;
import lavs.business.service.RetryPolicy;
import lavs.business.service.SyncListener;
import lavs.data.JsonDraftRepository;
import lavs.infrastructure.concurrent.ExecutorScheduler;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * A walk through the offline sync, for showing it to other people.
 *
 * <p>It logs in to the mainframe, saves a loan while the signal is switched off,
 * shows the loan waiting on the laptop, closes and reopens the application, then
 * brings the signal back and shows the loan arriving on the mainframe exactly once.
 * It uses a temporary folder for its drafts and removes it at the end.
 *
 * <p>Run it with no arguments to be asked for the password, or give
 * {@code --url}, {@code --user}, {@code --password}, {@code --customer} and
 * {@code --auto} (no pauses). The password can also come from the
 * {@code LAVS_PASSWORD} environment variable.
 *
 * <p><strong>It creates one real loan on the mainframe it connects to.</strong>
 */
public final class OfflineSyncDemo {

    /** Waits for the presenter before the next step. */
    public interface Pause {

        /** A pause that never waits. */
        Pause NONE = prompt -> { };

        /**
         * Waits for the presenter.
         *
         * @param prompt what to show while waiting
         */
        void await(String prompt);
    }

    private static final String DEFAULT_URL = "https://animating-eraser-arrogance.ngrok-free.dev/";
    private static final String DEFAULT_USER = "Group_2";
    private static final String DEFAULT_CUSTOMER = "01-0001-0000020";
    private static final long WAIT_SECONDS = 30;

    /** Tells the demo what the background sync reports, and lets it wait for it. */
    private final class Watcher implements SyncListener {
        private volatile CountDownLatch lost = new CountDownLatch(1);
        private volatile CountDownLatch arrived = new CountDownLatch(1);

        void reset() {
            lost = new CountDownLatch(1);
            arrived = new CountDownLatch(1);
        }

        @Override
        public void onConnectionLost(SyncResult result, Duration retryIn) {
            out.println("      [app] No connection. The record stays saved. Trying again in about "
                    + retryIn.toMillis() + " ms.");
            lost.countDown();
        }

        @Override
        public void onSyncFinished(SyncResult result) {
            if (result.synced() > 0) {
                out.println("      [app] Sent " + result.synced() + " record to the mainframe.");
                arrived.countDown();
            }
        }

        @Override
        public void onSyncFailed(RuntimeException error) {
            out.println("      [app] Problem while syncing: " + error.getMessage());
        }
    }

    private final Connection real;
    private final String customerNumber;
    private final PrintStream out;
    private final Pause pause;
    private final Watcher watcher = new Watcher();

    /**
     * Creates the demo.
     *
     * @param real the connection to the mainframe, already logged in
     * @param customerNumber the customer the demo loan is for
     * @param out where the narration goes
     * @param pause how the demo waits for the presenter
     */
    public OfflineSyncDemo(Connection real, String customerNumber, PrintStream out, Pause pause) {
        this.real = real;
        this.customerNumber = customerNumber;
        this.out = out;
        this.pause = pause;
    }

    /**
     * Starts the demo from the command line.
     *
     * @param args see the class comment
     * @throws Exception if something goes wrong while talking to the mainframe
     */
    public static void main(String[] args) throws Exception {
        String url = option(args, "--url", DEFAULT_URL);
        String user = option(args, "--user", DEFAULT_USER);
        String customer = option(args, "--customer", DEFAULT_CUSTOMER);
        String password = option(args, "--password", System.getenv("LAVS_PASSWORD"));
        boolean auto = Stream.of(args).anyMatch("--auto"::equals);
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        if (password == null) {
            System.out.print("Password for " + user + ": ");
            password = keyboard.readLine();
        }
        Connection connection = new HttpConnection(url);
        Status status = connection.Login(user, password);
        if (!status.getWasSuccessful()) {
            System.out.println("Could not log in to " + url + ": " + status.getErrorMessage());
            System.exit(1);
        }
        Pause pause = auto ? Pause.NONE : prompt -> {
            System.out.print(prompt + " [press Enter] ");
            try {
                keyboard.readLine();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
        new OfflineSyncDemo(connection, customer, System.out, pause).run();
        connection.close();
    }

    /**
     * Runs the walk through.
     *
     * @throws IOException if the temporary folder cannot be used
     * @throws InterruptedException if the demo is interrupted while waiting
     * @throws IllegalStateException if the application does not react in time
     */
    public void run() throws IOException, InterruptedException {
        Path folder = Files.createTempDirectory("lavs-demo");
        SwitchableConnection connection = new SwitchableConnection(real);
        AppServices app = null;
        try {
            out.println("== Offline sync demo ==");
            out.println("The Mobile Mortgage Manager is at a customer's house, and has no signal.");
            app = start(folder, connection);
            pause.await("Step 1: switch the signal OFF and save a loan");

            connection.goOffline();
            Draft draft = app.loans().createLoan(CustomerReference.existing(customerNumber), loan());
            out.println("   Loan saved. Status: " + draft.getStatus() + "  (saved locally, not yet sent)");
            out.println("   It is a file on this laptop: " + folder.resolve(draft.getLocalId() + ".json"));
            out.println("   " + Files.readString(folder.resolve(draft.getLocalId() + ".json")));
            await(watcher.lost, "the app to notice there is no connection");
            pause.await("Step 2: the laptop battery dies. Close the app and open it again, still with no signal");

            app.close();
            watcher.reset();
            app = start(folder, connection);
            await(watcher.lost, "the reopened app to find the loan still waiting");
            out.println("   The loan survived the restart. Status: "
                    + savedStatus(folder, draft.getLocalId()));
            pause.await("Step 3: the signal comes BACK");

            connection.goOnline();
            await(watcher.arrived, "the loan to reach the mainframe");
            Draft sent = new JsonDraftRepository(folder).findById(draft.getLocalId()).orElseThrow();
            out.println("   Status: " + sent.getStatus() + ". The mainframe gave it loan number "
                    + sent.getServerNumber().orElse("(unknown)") + ".");
            out.println("   One loan was created. No duplicate, no retyping.");
        } finally {
            if (app != null) {
                app.close();
            }
            deleteFolder(folder);
        }
    }

    private AppServices start(Path folder, SwitchableConnection connection) {
        watcher.reset();
        CustomerProfileStandIn profiles = new CustomerProfileStandIn();
        return AppServices.start(folder, AppServices.mainframeGateways(connection), profiles,
                new BasicLoanValidator(), watcher, new ExecutorScheduler(),
                new RetryPolicy(Duration.ofMillis(500), Duration.ofSeconds(4), () -> 1.0));
    }

    private void await(CountDownLatch latch, String what) throws InterruptedException {
        if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Gave up waiting for " + what + ".");
        }
    }

    private static String savedStatus(Path folder, String draftId) {
        return new JsonDraftRepository(folder).findById(draftId).orElseThrow().getStatus().toString();
    }

    private static LoanDetails loan() {
        LocalDate start = LocalDate.now();
        return new LoanDetails(new BigDecimal("25000.00"), "Fixed", new BigDecimal("6.50"), "Yearly",
                new BigDecimal("500.00"), "Monthly", start, start.plusYears(5));
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private static void deleteFolder(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    /**
     * Stands in for the customer service, which does not exist yet, by saying every
     * customer has a mailing address, a way to be contacted and an employer.
     */
    private static final class CustomerProfileStandIn implements lavs.business.service.CustomerProfileSource {
        @Override
        public Optional<CustomerProfile> profileOf(CustomerReference customer) {
            return Optional.of(new CustomerProfile(true, true, true));
        }
    }
}
