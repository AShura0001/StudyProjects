package lavs.infrastructure.mainframe;

import lavs.business.model.LoanDocument;
import lavs.business.repository.LoanDocumentGateway;
import lavs.business.repository.PageResult;
import mainframe.client.Connection;
import mainframe.client.messages.loan.AddDocument;
import mainframe.client.messages.loan.DeleteDocument;
import mainframe.client.messages.loanDocument.List;
import mainframe.client.messages.loanDocument.Retrieve;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Talks to the mainframe for {@link LoanDocumentGateway}.
 */
public class LoanDocumentAdapter implements LoanDocumentGateway {

    private final Connection connection;

    public LoanDocumentAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<LoanDocument> findDocument(String loanNumber, long id) {
        Retrieve retrieve = new Retrieve(id, loanNumber);
        if (!MainframeCalls.send(retrieve, connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return Optional.of(new LoanDocument(
                retrieve.getId(),
                retrieve.getLoanNumber() == null ? loanNumber : retrieve.getLoanNumber(),
                retrieve.getFileName(),
                retrieve.getFileType(),
                retrieve.getSize(),
                retrieve.getType()));
    }

    @Override
    public PageResult<LoanDocument> listDocuments(String loanNumber, long page, long pageSize) {
        List list = new List(loanNumber, page, pageSize);
        if (!MainframeCalls.send(list, connection).getWasSuccessful()) {
            return PageResult.empty(page);
        }
        String listLoanNumber = list.getLoanNumber() == null ? loanNumber : list.getLoanNumber();
        java.util.List<LoanDocument> documents = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            documents.add(new LoanDocument(
                    item.getId(), listLoanNumber, item.getFileName(), null, null, item.getType()));
        }
        return new PageResult<>(documents, list.getTotal(), list.getPage());
    }

    @Override
    public LoanDocument addDocument(String loanNumber, String fileName, String fileType, long size, String type) {
        AddDocument add = new AddDocument(fileName, fileType, loanNumber, size, type);
        MainframeCalls.sendChange(add, connection);
        return findDocument(loanNumber, add.getId())
                .orElseGet(() -> new LoanDocument(add.getId(), loanNumber, fileName, fileType, size, type));
    }

    @Override
    public void deleteDocument(String loanNumber, long id) {
        MainframeCalls.sendChange(new DeleteDocument(id, loanNumber), connection);
    }
}
