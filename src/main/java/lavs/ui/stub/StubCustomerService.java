package lavs.ui.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Temporary stand-in for the business layer's CustomerService. Blocking on
 * purpose so callers must keep it off the JavaFX thread.
 */
public class StubCustomerService {

    public record Match(String name, String customerNumber) {
    }

    public static final int TOTAL_CUSTOMERS = 6767;

    private static final String[] FIRST_NAMES = {
        "Alex", "Bree", "Caleb", "Dana", "Elio", "Freya", "Gus", "Hana", "Ivo", "Jess",
        "Kai", "Lena", "Milo", "Nora", "Omar", "Pia", "Quinn", "Rosa", "Sven", "Tara"};

    private static final String[] LAST_NAMES = {
        "Adams", "Baker", "Clarke", "Dixon", "Evans", "Foster", "Grant", "Hughes", "Ito", "Jensen",
        "Khan", "Lopez", "Murray", "Nguyen", "Owens", "Patel", "Quinn", "Reyes", "Singh", "Turner"};

    private static final List<Match> DIRECTORY = buildDirectory();

    private final long simulatedLatencyMillis;

    public StubCustomerService() {
        this(600);
    }

    public StubCustomerService(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    public List<Match> search(String term) {
        if (!pause()) {
            return List.of();
        }
        String needle = term.trim().toLowerCase(Locale.ROOT);
        return DIRECTORY.stream()
                .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(needle)
                        || m.customerNumber().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public int count() {
        return DIRECTORY.size();
    }

    /** @param page zero-based page index */
    public List<Match> page(int page, int size) {
        if (!pause()) {
            return List.of();
        }
        int from = Math.min(page * size, DIRECTORY.size());
        int to = Math.min(from + size, DIRECTORY.size());
        return DIRECTORY.subList(from, to);
    }

    private boolean pause() {
        try {
            Thread.sleep(simulatedLatencyMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static List<Match> buildDirectory() {
        List<Match> all = new ArrayList<>(TOTAL_CUSTOMERS);
        all.add(new Match("Jane Doe", "CUS-20481"));
        all.add(new Match("Mary Jane", "CUS-20482"));
        all.add(new Match("Sarah Doe Jr. Watkins", "CUS-20483"));
        all.add(new Match("Michael Torres", "CUS-20484"));
        all.add(new Match("Priya Sharma", "CUS-20485"));
        all.add(new Match("David Kim", "CUS-20486"));
        all.add(new Match("Olivia Chen", "CUS-20487"));
        all.add(new Match("James O'Brien", "CUS-20488"));
        all.add(new Match("Fatima Al-Hassan", "CUS-20489"));
        all.add(new Match("Lucas Andersson", "CUS-20490"));
        for (int i = all.size(); i < TOTAL_CUSTOMERS; i++) {
            String first = FIRST_NAMES[i % FIRST_NAMES.length];
            String last = LAST_NAMES[(i / FIRST_NAMES.length) % LAST_NAMES.length];
            all.add(new Match(first + " " + last, "CUS-" + (20481 + i)));
        }
        return List.copyOf(all);
    }
}
