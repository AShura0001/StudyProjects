package lavs.business.repository;

import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loanCoborrower.Retrieve;

import java.io.IOException;
import java.util.Optional;

public interface LoanCoBorrowerGateway {

    Optional<Retrieve> findCoBorrowerByLoanNumberAndId(long id, String loanNumber) throws IOException;

    Optional<mainframe.client.messages.loanCoborrower.List> findCoBorrowersByLoanNumber(String loanNumber, long page, long size) throws IOException;

    Optional<AddCoborrower> addLoanCoBorrower(AddCoborrower coBorrower) throws IOException;

    boolean deleteLoanCoBorrowerByLoanNumberAndId(long id, String loanNumber) throws IOException;
}
