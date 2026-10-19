package lavs.ui.service;

import lavs.business.model.RepaymentEntry;
import lavs.business.repository.PageResult;
import lavs.infrastructure.mainframe.LoanAdapter;
import lavs.infrastructure.mainframe.LoanCoBorrowerAdapter;
import lavs.infrastructure.mainframe.LoanPaymentAdapter;
import mainframe.client.Connection;

import java.math.BigDecimal;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The loan screen's data from the real mainframe. Fields the screen shows that
 * the mainframe does not hold (a loan name, an outstanding balance) are worked
 * out from the schedule and payments instead.
 */
public class MainframeLoanRecords implements LoanRecords {

    private static final long LIST_SIZE = 50;

    private final MainframeLoanLookup lookup;
    private final LoanAdapter loans;
    private final LoanCoBorrowerAdapter coBorrowers;
    private final LoanPaymentAdapter payments;

    public MainframeLoanRecords(Connection connection) {
        Objects.requireNonNull(connection, "connection must not be null");
        this.lookup = new MainframeLoanLookup(connection);
        this.loans = new LoanAdapter(connection);
        this.coBorrowers = new LoanCoBorrowerAdapter(connection);
        this.payments = new LoanPaymentAdapter(connection);
    }

    @Override
    public LoanDetails details(String loanNumber) {
        MainframeLoanLookup.Facts facts = lookup.facts(loanNumber);
        if (facts == null) {
            return null;
        }

        List<RepaymentEntry> schedule = loans.findRepaymentSchedule(loanNumber);
        int paid = (int) Math.min(schedule.size(), payments.listPayments(loanNumber, 1, 1).getTotal());

        BigDecimal principalPaid = BigDecimal.ZERO;
        BigDecimal interestPaid = BigDecimal.ZERO;
        List<ScheduleEntry> rows = new ArrayList<>();
        for (int i = 0; i < schedule.size(); i++) {
            RepaymentEntry entry = schedule.get(i);
            String status = i < paid ? "Paid" : i == paid ? "Due" : "Upcoming";
            if (i < paid) {
                principalPaid = principalPaid.add(entry.principalAmount());
                interestPaid = interestPaid.add(entry.interestAmount());
            }
            rows.add(new ScheduleEntry((int) entry.paymentNumber(), entry.date().toString(),
                    money(entry.principalAmount()), money(entry.interestAmount()),
                    money(entry.amountPaid()), status));
        }

        BigDecimal principal = BigDecimal.valueOf(facts.principal());
        BigDecimal remaining = principal.subtract(principalPaid).max(BigDecimal.ZERO);
        String nextDue = paid < schedule.size() ? schedule.get(paid).date().toString() : "None";

        PageResult<lavs.business.model.CoBorrower> coBorrowerPage = coBorrowers.listCoBorrowers(loanNumber, 1, LIST_SIZE);
        List<LoanRecords.CoBorrower> people = coBorrowerPage.getList().stream()
                .map(c -> new LoanRecords.CoBorrower(
                        c.coBorrowerName() == null ? c.coBorrowerNumber() : c.coBorrowerName(),
                        c.coBorrowerNumber()))
                .toList();

        String suffix = loanNumber.contains("-") ? loanNumber.substring(loanNumber.lastIndexOf('-') + 1) : loanNumber;
        return new LoanDetails(loanNumber, "Loan " + suffix, money(principal),
                String.format(Locale.US, "%.2f%% %s (%s)", facts.rateAmount(), nz(facts.rateType()), nz(facts.rateFrequency())),
                term(facts), text(facts.startDate()), text(facts.renewalDate()),
                money(BigDecimal.valueOf(facts.paymentAmount())) + " / " + nz(facts.paymentFrequency()),
                money(remaining), facts.status() == null ? "Unknown" : facts.status(), paid, schedule.size(),
                money(principalPaid.add(interestPaid)), money(principalPaid), money(interestPaid),
                money(remaining), nextDue, people, List.copyOf(rows));
    }

    @Override
    public Map<String, String> save(LoanDetails loan) {
        throw new NotSupportedYetException(
                "Saving loan changes is not connected to the mainframe yet. Your changes are still on screen.");
    }

    private static String term(MainframeLoanLookup.Facts facts) {
        if (facts.startDate() == null || facts.renewalDate() == null) {
            return "";
        }
        Period period = Period.between(facts.startDate(), facts.renewalDate());
        StringBuilder text = new StringBuilder();
        if (period.getYears() > 0) {
            text.append(period.getYears()).append(period.getYears() == 1 ? " year" : " years");
        }
        if (period.getMonths() > 0) {
            text.append(text.isEmpty() ? "" : " ").append(period.getMonths()).append(period.getMonths() == 1 ? " month" : " months");
        }
        if (text.isEmpty()) {
            text.append(period.getDays()).append(" days");
        }
        return text.toString();
    }

    private static String text(java.time.LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.US, "$%,.2f", value);
    }
}
