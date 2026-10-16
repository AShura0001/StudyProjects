package lavs.ui.stub;

import java.util.List;
import java.util.Locale;

/**
 * Temporary stand-in for the business layer's CustomerService. Blocking on
 * purpose so callers must keep it off the JavaFX thread.
 */
public class StubCustomerService {

    public record Match(String name, String customerNumber) {
    }

    private static final List<Match> DIRECTORY = List.of(
            new Match("Jane Doe", "CUS-20481"),
            new Match("Mary Jane", "CUS-20482"),
            new Match("Sarah Doe Jr. Watkins", "CUS-20483"),
            new Match("Michael Torres", "CUS-20484"),
            new Match("Priya Sharma", "CUS-20485"));

    private final long simulatedLatencyMillis;

    public StubCustomerService() {
        this(600);
    }

    public StubCustomerService(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    public List<Match> search(String term) {
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
        String needle = term.trim().toLowerCase(Locale.ROOT);
        return DIRECTORY.stream()
                .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(needle)
                        || m.customerNumber().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }
}
