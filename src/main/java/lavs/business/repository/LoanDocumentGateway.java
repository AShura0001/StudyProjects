package lavs.business.repository;

import mainframe.client.messages.loan.AddDocument;
import mainframe.client.messages.loanDocument.Retrieve;

import java.io.IOException;
import java.util.Optional;

public interface LoanDocumentGateway {

    Optional<Retrieve> findLoanDocumentByLoanNumberAndId(long id, String loanNumber) throws IOException;

    Optional<mainframe.client.messages.loanDocument.List> findLoanDocumentsByLoanNumber(String loanNumber, long page, long size) throws IOException;

    Optional<AddDocument> addLoanDocument(AddDocument document) throws IOException;

    boolean deleteLoanDocumentByLoanNumberAndId(long id, String loanNumber) throws IOException;
}
