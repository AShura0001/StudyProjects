package lavs.infrastructure.mainframe;

import lavs.business.model.LoanPayment;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.PageResult;
import mainframe.client.Connection;
import mainframe.client.messages.loan.AddPayment;
import mainframe.client.messages.loan.DeletePayment;
import mainframe.client.messages.loanPayment.List;
import mainframe.client.messages.loanPayment.Retrieve;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static lavs.infrastructure.mainframe.MainframeCalls.date;
import static lavs.infrastructure.mainframe.MainframeCalls.money;
import static lavs.infrastructure.mainframe.MainframeCalls.required;

/**
 * Talks to the mainframe for {@link LoanPaymentGateway}.
 */
public class LoanPaymentAdapter implements LoanPaymentGateway {

    private final Connection connection;

    public LoanPaymentAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<LoanPayment> findPayment(String loanNumber, long id) {
        Retrieve retrieve = new Retrieve(id, loanNumber);
        if (!MainframeCalls.send(retrieve, connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return Optional.of(new LoanPayment(
                retrieve.getId(),
                retrieve.getLoanNumber() == null ? loanNumber : retrieve.getLoanNumber(),
                required(date(retrieve.getDate()), "the payment date"),
                required(money(retrieve.getAmountPaid()), "the amount paid"),
                money(retrieve.getPrincipalAmount()),
                money(retrieve.getInterestAmount()),
                money(retrieve.getAmountRemaining())));
    }

    @Override
    public PageResult<LoanPayment> listPayments(String loanNumber, long page, long pageSize) {
        List list = new List(loanNumber, page, pageSize);
        if (!MainframeCalls.send(list, connection).getWasSuccessful()) {
            return PageResult.empty(page);
        }
        String listLoanNumber = list.getLoanNumber() == null ? loanNumber : list.getLoanNumber();
        java.util.List<LoanPayment> payments = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            payments.add(new LoanPayment(
                    item.getId(),
                    listLoanNumber,
                    required(date(item.getDate()), "a payment date"),
                    required(money(item.getAmountPaid()), "an amount paid"),
                    null,
                    null,
                    null));
        }
        return new PageResult<>(payments, list.getTotal(), list.getPage());
    }

    @Override
    public LoanPayment addPayment(String loanNumber, LocalDate date, BigDecimal amountPaid) {
        AddPayment add = new AddPayment(amountPaid.doubleValue(), date.toString(), loanNumber);
        MainframeCalls.sendChange(add, connection);
        return new LoanPayment(
                add.getId(),
                add.getLoanNumber() == null ? loanNumber : add.getLoanNumber(),
                date,
                amountPaid,
                money(add.getPrincipalAmount()),
                money(add.getInterestAmount()),
                money(add.getAmountRemaining()));
    }

    @Override
    public void deletePayment(String loanNumber, long id) {
        MainframeCalls.sendChange(new DeletePayment(id, loanNumber), connection);
    }
}
