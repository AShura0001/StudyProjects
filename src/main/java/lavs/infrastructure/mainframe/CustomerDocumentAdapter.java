package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.model.Document;
import lavs.business.repository.CustomerDocumentGateway;
import lavs.business.repository.CustomerGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.AddDocument;
import mainframe.client.messages.customerDocument.Retrieve;
import mainframe.client.messages.loan.DeleteDocument;

import java.io.IOException;
import java.util.Optional;

public class CustomerDocumentAdapter implements CustomerDocumentGateway {
    private final Connection connection;

    public CustomerDocumentAdapter(Connection connection){
            this.connection = connection;
    }
    @Override
    public Optional<Document> findDocumentByCustomerNumberAndId(Long id, String customerNumber) throws IOException {
        return Optional.empty();
    }


    @Override
    public boolean deleteCustomerDocument( long id,String customerNumber) throws IOException {
        DeleteDocument deleteDocument = new DeleteDocument(id, customerNumber);
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
        if(addDocument.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(addDocument.getId(),  addDocument.getCustomer());
            return mapMessageToDocument(retrieve);
        }
        return Optional.empty();
    }

    public Optional<Document> mapMessageToDocument(Message message){
        if(message instanceof Retrieve retrieve){
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
