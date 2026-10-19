package lavs.infrastructure.mainframe;

import lavs.business.model.Relationship;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.AddRelationship;
import mainframe.client.messages.customer.DeleteRelationship;
import mainframe.client.messages.customer.UpdateRelationship;
import mainframe.client.messages.customerRelationship.List;
import mainframe.client.messages.customerRelationship.Retrieve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerRelationshipAdapter implements CustomerRelationshipGateway {
    private final Connection connection;

    public CustomerRelationshipAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Relationship> findRelationshipByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        if (retrieve.send(connection).getWasSuccessful()) {
            return mapMessageToRelationship(retrieve);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Relationship> updateCustomerRelationship(Relationship relationship) throws IOException {
        UpdateRelationship updateRelationship = new UpdateRelationship(
                relationship.get_customerNumber(),
                relationship.get_id(),
                relationship.get_otherType(),
                relationship.get_secondaryCustomerNumber(),
                relationship.get_type()
        );
        if (updateRelationship.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(relationship.get_id(), relationship.get_customerNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToRelationship(retrieve);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean deleteRelationshipByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        DeleteRelationship deleteRelationship = new DeleteRelationship(customerNumber, id);
        return deleteRelationship.send(connection).getWasSuccessful();
    }

    @Override
    public Optional<Relationship> addCustomerRelationship(Relationship relationship) throws IOException {
        AddRelationship addRelationship = new AddRelationship(
                relationship.get_customerNumber(),
                relationship.get_otherType(),
                relationship.get_secondaryCustomerNumber(),
                relationship.get_type()
        );
        if (addRelationship.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(addRelationship.getId(), relationship.get_customerNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToRelationship(retrieve);
            }
        }
        return Optional.empty();
    }

    @Override
    public CustomerSearchResult<Relationship> ListCustomerRelationships(long page, String customerNumber, long page_size) throws IOException {
        List list = new List(customerNumber, page, page_size);
        if (!list.send(connection).getWasSuccessful()) {
            return new CustomerSearchResult<>(
                    java.util.List.of(),
                    0,
                    page
            );
        }

        return new CustomerSearchResult<>(
                mapItemsToRelationshipList(list),
                list.getTotal(),
                list.getPage()
        );
    }

    private ArrayList<Relationship> mapItemsToRelationshipList(List list) {
        ArrayList<Relationship> items = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            Relationship relationship = new Relationship();
            relationship.set_id(item.getId());
            relationship.set_secondaryCustomerNumber(item.getSecondaryCustomerNumber());
            relationship.set_type(item.getType());
            relationship.set_customerNumber(list.getCustomerNumber());
            relationship.set_customerName(list.getCustomerName());
            items.add(relationship);
        }
        return items;
    }

    public Optional<Relationship> mapMessageToRelationship(Message message) {
        if (message instanceof Retrieve retrieve) {
            return Optional.of(
                    new Relationship(
                            retrieve.getCustomerName(),
                            retrieve.getCustomerNumber(),
                            retrieve.getId(),
                            retrieve.getOtherType(),
                            retrieve.getSecondaryCustomerName(),
                            retrieve.getSecondaryCustomerNumber(),
                            retrieve.getType()
                    ));
        }
        return Optional.empty();
    }
}
