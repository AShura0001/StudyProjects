package lavs.infrastructure.mainframe;

import lavs.business.model.Relationship;
import lavs.business.repository.CustomerRelationshipGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.DeleteRelationship;
import mainframe.client.messages.customer.UpdateRelationship;
import mainframe.client.messages.customerRelationship.Retrieve;

import java.io.IOException;
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
                relationship.get_customerName(),
                relationship.get_id(),
                relationship.get_otherType(),
                relationship.get_secondaryCustomerNumber(),
                relationship.get_type()
        );
        if (updateRelationship.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(relationship.get_id(), relationship.get_customerNumber());
            return mapMessageToRelationship(retrieve);
        }

        return Optional.empty();
    }

    @Override
    public boolean deleteRelationshipByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        DeleteRelationship deleteRelationship = new DeleteRelationship(customerNumber, id);
        return deleteRelationship.send(connection).getWasSuccessful();
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
