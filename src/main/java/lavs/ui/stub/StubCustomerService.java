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

    public record Employer(String name, String id) {
    }

    public record Loan(String name, String date, String loanNumber) {
    }

    public record Details(String customerNumber, String firstName, String lastName, String phone,
                          String email, String addressLine1, String addressLine2, String notes,
                          List<Employer> employers, List<Loan> loans) {
    }

    /** Returns sample details for any customer number found in the directory, or null. */
    public Details details(String customerNumber) {
        if (!pause()) {
            return null;
        }
        Match match = DIRECTORY.stream()
                .filter(m -> m.customerNumber().equals(customerNumber))
                .findFirst()
                .orElse(null);
        if (match == null) {
            return null;
        }
        String[] parts = match.name().split(" ", 2);
        String first = parts[0];
        String last = parts.length > 1 ? parts[1] : "";
        String handle = (first + "." + last).toLowerCase(Locale.ROOT).replaceAll("[^a-z.]", "");
        return new Details(match.customerNumber(), first, last, "+64 21 555 0142",
                handle + "@example.com", "14 Karaka Street", "Newmarket, Auckland 1023",
                "Prefers contact by email. Mortgage review due in March.",
                List.of(new Employer("Sarah Jenkins", "CMP-1001"),
                        new Employer("Michael Torres", "CMP-1002"),
                        new Employer("Priya Sharma", "CMP-1003"),
                        new Employer("David Kim", "CMP-1004"),
                        new Employer("Emily Chen", "CMP-1005")),
                List.of(new Loan("Home Mortgage", "2023-01-15", "LN-5001"),
                        new Loan("Auto Loan", "2024-03-22", "LN-5002"),
                        new Loan("Personal Loan", "2024-08-10", "LN-5003"),
                        new Loan("Student Loan", "2022-09-01", "LN-5004")));
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
