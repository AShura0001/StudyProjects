package lavs.business.repository;

import java.io.IOException;
import java.util.Optional;

public interface LoanRepaymentScheduleGateway {

    Optional<mainframe.client.messages.loanRepaymentSchedule.List> findLoanRepaymentScheduleByLoanNumber(String loanNumber, long page, long size) throws IOException;
}
