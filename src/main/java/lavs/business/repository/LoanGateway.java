package lavs.business.repository;

import mainframe.client.messages.loan.Create;
import mainframe.client.messages.loan.Retrieve;
import mainframe.client.messages.loan.Update;

import java.io.IOException;
import java.util.Optional;

public interface LoanGateway {

    Optional<Retrieve> findLoanByNumber(String number) throws IOException;

    Optional<mainframe.client.messages.loan.List> findLoansByCustomerNumber(String customerNumber, long page, long size) throws IOException;

    Optional<Create> createLoan(Create loan) throws IOException;

    Optional<Update> updateLoan(Update loan) throws IOException;

    boolean updateLoanStatus(String loanNumber, String status) throws IOException;
}
