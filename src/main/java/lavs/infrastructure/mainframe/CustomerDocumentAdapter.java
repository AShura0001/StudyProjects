package lavs.infrastructure.mainframe;

import lavs.business.model.Document;
import lavs.business.repository.CustomerDocumentGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.AddDocument;
import mainframe.client.messages.customerDocument.List;
import mainframe.client.messages.customerDocument.Retrieve;
import mainframe.client.messages.customer.DeleteDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerDocumentAdapter implements CustomerDocumentGateway {
    private final Connection connection;

    public CustomerDocumentAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Document> findDocumentByCustomerNumberAndId(Long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        if(retrieve.send(connection).getWasSuccessful()){
            return mapMessageToDocument(retrieve);
        }
        return Optional.empty();
    }


    @Override
    public boolean deleteCustomerDocument(long id, String customerNumber) throws IOException {
        DeleteDocument deleteDocument = new DeleteDocument(customerNumber, id);
        return deleteDocument.send(connection).getWasSuccessful();
    }

    @Override
    public Optional<Document> addCustomerDocument(Document document) throws IOException {
        AddDocument addDocument = new AddDocument(
                document.getCustomerNumber(),
                document.getFileName(),
                document.getFileType(),
                document.getSize(),
                document.getType()
        );
        if (addDocument.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(addDocument.getId(), document.getCustomerNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToDocument(retrieve);
            }
        }
        return Optional.empty();
    }

    @Override
    public CustomerSearchResult<Document> ListCustomerDocuments(long page, String customerNumber, long page_size) throws IOException {
        List list = new List(customerNumber, page, page_size);
        if (!list.send(connection).getWasSuccessful()) {
            return new CustomerSearchResult<>(
                    java.util.List.of(),
                    0,
                    page
            );
        }

        ArrayList<Document> documents =
                mapItemsToDocumentList(list);

        return new CustomerSearchResult<>(
                documents,
                list.getTotal(),
                list.getPage()
        );
    }

    private ArrayList<Document> mapItemsToDocumentList(List list) {
        ArrayList<Document> documents = new ArrayList<>();
        for (List.Item item: list.getItems()) {
            Document document = new Document();
            document.setCustomerName(list.getCustomerName());
            document.setCustomerNumber(list.getCustomerNumber());
            document.setNumber(list.getCustomerNumber());
            document.setFileName(item.getFileName());
            document.setId(item.getId());
            document.setType(item.getType());
            documents.add(document);
        }
        return documents;
    }


    public Optional<Document> mapMessageToDocument(Message message) {
        if (message instanceof Retrieve retrieve) {
            Document document = new Document(
                    retrieve.getCustomerName(),
                    retrieve.getCustomerNumber(),
                    retrieve.getFileName(),
                    retrieve.getFileType(),
                    retrieve.getId(),
                    retrieve.getSize(),
                    retrieve.getType()
            );
            return Optional.of(document);
        }

        return Optional.empty();
    }
}
