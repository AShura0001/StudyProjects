package lavs.business.repository;

import mainframe.client.messages.loan.AddPayment;
import mainframe.client.messages.loanPayment.Retrieve;

import java.io.IOException;
import java.util.Optional;

public interface LoanPaymentGateway {

    Optional<Retrieve> findLoanPaymentByLoanNumberAndId(long id, String loanNumber) throws IOException;

    Optional<mainframe.client.messages.loanPayment.List> findLoanPaymentsByLoanNumber(String loanNumber, long page, long size) throws IOException;

    Optional<AddPayment> addLoanPayment(AddPayment payment) throws IOException;

    boolean deleteLoanPaymentByLoanNumberAndId(long id, String loanNumber) throws IOException;
}
