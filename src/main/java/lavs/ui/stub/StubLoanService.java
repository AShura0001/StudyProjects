package lavs.ui.stub;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Temporary stand-in for the business layer's LoanService. Blocking on
 * purpose so callers must keep it off the JavaFX thread. The schedule is a
 * real amortisation of the sample loan so the numbers add up.
 */
public class StubLoanService {

    public record CoBorrower(String name, String customerNumber) {
    }

    public record ScheduleEntry(int number, String dueDate, String principal, String interest,
                                String total, String status) {
    }

    public record LoanDetails(String loanNumber, String loanName, String loanAmount,
                              String interestRate, String loanTerm, String startDate,
                              String maturityDate, String monthlyPayment, String outstandingBalance,
                              String statusText, int paymentsMade, int paymentsTotal,
                              String totalPaid, String principalPaid, String interestPaid,
                              String remainingBalance, String nextPaymentDue,
                              List<CoBorrower> coBorrowers, List<ScheduleEntry> schedule) {
    }

    private record Sample(String loanNumber, String name, double amount, double rate, int months,
                          LocalDate start, List<CoBorrower> coBorrowers) {
    }

    /** The date the sample data is "as of", so the schedule status is repeatable. */
    private static final LocalDate AS_OF = LocalDate.of(2024, 10, 1);

    private static final List<Sample> SAMPLES = List.of(
            new Sample("LN-5001", "Home Mortgage", 450000, 5.85, 300, LocalDate.of(2023, 1, 15),
                    List.of(new CoBorrower("Jane Doe", "CUS-20481"))),
            new Sample("LN-5002", "Auto Loan", 28000, 4.90, 60, LocalDate.of(2024, 3, 22), List.of()),
            new Sample("LN-5003", "Personal Loan", 12000, 9.50, 36, LocalDate.of(2024, 8, 10), List.of()),
            new Sample("LN-5004", "Student Loan", 45000, 5.25, 120, LocalDate.of(2022, 9, 1),
                    List.of(new CoBorrower("Jane Doe", "CUS-20481"),
                            new CoBorrower("Michael Torres", "CUS-20484"),
                            new CoBorrower("Olivia Chen", "CUS-20487"))));

    private final long simulatedLatencyMillis;

    public StubLoanService() {
        this(350);
    }

    public StubLoanService(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    /** @return the loan, or null when the number is unknown */
    public LoanDetails details(String loanNumber) {
        pause();
        return SAMPLES.stream()
                .filter(s -> s.loanNumber().equals(loanNumber))
                .findFirst()
                .map(StubLoanService::build)
                .orElse(null);
    }

    /**
     * Stand-in for the business layer's loan rules. Returns field name to
     * message; an empty map means the save was accepted.
     */
    public Map<String, String> save(LoanDetails loan) {
        pause();
        Map<String, String> errors = new LinkedHashMap<>();
        if (loan.loanName().isBlank()) {
            errors.put("loanName", "Loan name is required.");
        }
        if (!(number(loan.loanAmount()) > 0)) {
            errors.put("loanAmount", "Loan amount must be greater than zero.");
        }
        double rate = number(loan.interestRate());
        if (Double.isNaN(rate) || rate < 0 || rate > 100) {
            errors.put("interestRate", "Interest rate must be between 0% and 100%.");
        }
        if (loan.loanTerm().isBlank()) {
            errors.put("loanTerm", "Loan term is required.");
        }
        LocalDate start = date(loan.startDate());
        LocalDate maturity = date(loan.maturityDate());
        if (start == null) {
            errors.put("startDate", "Start date must look like 2022-09-01.");
        }
        if (maturity == null) {
            errors.put("maturityDate", "Maturity date must look like 2032-09-01.");
        } else if (start != null && !maturity.isAfter(start)) {
            errors.put("maturityDate", "Maturity date must be after the start date.");
        }
        if (!(number(loan.monthlyPayment()) > 0)) {
            errors.put("monthlyPayment", "Monthly payment must be greater than zero.");
        }
        return errors;
    }

    private static LoanDetails build(Sample s) {
        double r = s.rate() / 100 / 12;
        double payment = round(s.amount() * r / (1 - Math.pow(1 + r, -s.months())));

        List<ScheduleEntry> schedule = new ArrayList<>();
        double balance = s.amount();
        double principalPaid = 0;
        double interestPaid = 0;
        int paid = 0;
        String nextDue = "None";
        boolean dueMarked = false;

        for (int i = 1; i <= s.months(); i++) {
            LocalDate due = s.start().plusMonths(i);
            double interest = round(balance * r);
            double principal = i == s.months() ? round(balance) : round(payment - interest);
            double total = round(principal + interest);
            balance = round(balance - principal);

            String status;
            if (due.isBefore(AS_OF)) {
                status = "Paid";
                paid++;
                principalPaid += principal;
                interestPaid += interest;
            } else if (!dueMarked) {
                status = "Due";
                nextDue = due.toString();
                dueMarked = true;
            } else {
                status = "Upcoming";
            }
            schedule.add(new ScheduleEntry(i, due.toString(), money(principal), money(interest),
                    money(total), status));
        }

        double remaining = round(s.amount() - principalPaid);
        String term = s.months() % 12 == 0
                ? s.months() + " months (" + s.months() / 12 + " years)"
                : s.months() + " months";

        return new LoanDetails(s.loanNumber(), s.name(), money(s.amount()),
                String.format(Locale.US, "%.2f%%", s.rate()), term, s.start().toString(),
                s.start().plusMonths(s.months()).toString(), money(payment), money(remaining),
                "Active — In Good Standing", paid, s.months(),
                money(principalPaid + interestPaid), money(principalPaid), money(interestPaid),
                money(remaining), nextDue, s.coBorrowers(), List.copyOf(schedule));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String money(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    private static double number(String text) {
        try {
            return Double.parseDouble(text.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static LocalDate date(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void pause() {
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
