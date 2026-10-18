package lavs.business.repository;

import mainframe.client.messages.loanSummary.Retrieve;

import java.io.IOException;
import java.util.Optional;

public interface LoanSummaryGateway {

    Optional<Retrieve> findLoanSummaryByLoanNumber(String loanNumber) throws IOException;
}
