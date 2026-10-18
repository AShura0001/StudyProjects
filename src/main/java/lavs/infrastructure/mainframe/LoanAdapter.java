package lavs.infrastructure.mainframe;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.LoanGateway;
import mainframe.client.Connection;
import mainframe.client.messages.loan.UpdateStatus;
import mainframe.client.messages.loanRepaymentSchedule.List;
import mainframe.client.messages.loanSummary.Retrieve;

import java.util.ArrayList;
import java.util.Optional;

import static lavs.infrastructure.mainframe.MainframeCalls.date;
import static lavs.infrastructure.mainframe.MainframeCalls.money;
import static lavs.infrastructure.mainframe.MainframeCalls.required;

/**
 * Talks to the mainframe for {@link LoanGateway}.
 */
public class LoanAdapter implements LoanGateway {

    /** How many schedule lines to ask for at a time. */
    static final long SCHEDULE_PAGE_SIZE = 100;

    /** Stops paging if the mainframe keeps sending pages, so a bad total can't loop forever. */
    private static final int MAX_SCHEDULE_PAGES = 1000;

    private final Connection connection;

    public LoanAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<LoanSummary> findSummary(String loanNumber) {
        Retrieve retrieve = new Retrieve(loanNumber);
        if (!MainframeCalls.send(retrieve, connection).getWasSuccessful()
                || retrieve.getLoanNumber() == null) {
            return Optional.empty();
        }
        return Optional.of(new LoanSummary(
                retrieve.getLoanNumber(),
                required(retrieve.getCustomerName(), "the customer name"),
                retrieve.getNumberOfPayments(),
                required(money(retrieve.getTotalAmountPaid()), "the total amount paid"),
                required(money(retrieve.getTotalInterestPaid()), "the total interest paid"),
                required(date(retrieve.getFinalPaymentDate()), "the final payment date"),
                required(money(retrieve.getFinalPaymentAmount()), "the final payment amount")));
    }

    @Override
    public java.util.List<RepaymentEntry> findRepaymentSchedule(String loanNumber) {
        java.util.List<RepaymentEntry> entries = new ArrayList<>();
        for (long page = 1; page <= MAX_SCHEDULE_PAGES; page++) {
            List list = new List(loanNumber, page, SCHEDULE_PAGE_SIZE);
            if (!MainframeCalls.send(list, connection).getWasSuccessful()) {
                break;
            }
            for (List.Item item : list.getItems()) {
                entries.add(new RepaymentEntry(
                        item.getPaymentNumber(),
                        required(date(item.getDate()), "a repayment date"),
                        money(item.getAmountPaid()),
                        money(item.getPrincipalAmount()),
                        money(item.getInterestAmount()),
                        money(item.getAmountRemaining())));
            }
            if (list.getItems().length == 0 || entries.size() >= list.getTotal()) {
                break;
            }
        }
        return java.util.List.copyOf(entries);
    }

    @Override
    public void submitForChecking(String loanNumber) {
        MainframeCalls.sendChange(
                new UpdateStatus(loanNumber, UpdateStatus.STATUS_PENDING),
                connection);
    }
}
