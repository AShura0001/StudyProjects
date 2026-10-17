package lavs.infrastructure.mainframe;

import lavs.business.model.Email;
import lavs.business.repository.CustomerEmailGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.DeleteEmail;
import mainframe.client.messages.customer.UpdateEmail;
import mainframe.client.messages.customerEmail.Retrieve;


import java.io.IOException;
import java.util.Optional;

public class CustomerEmailAdapter implements CustomerEmailGateway {
    private final Connection connection;

    public CustomerEmailAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Email> findEmailByCustomerNumberAndId(Long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve();
        if (!retrieve.send(connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return mapMessageToEmail(retrieve);
    }


    @Override
    public Optional<Email> updateCustomerEmail(Email email) throws IOException {
        UpdateEmail updateEmail = new UpdateEmail(
                email.getAddress(),
                email.getCustomerName(),
                email.getId(),
                email.isPrimary(),
                email.getOtherType(),
                email.getType()
        );
        Status status = updateEmail.send(connection);
        if (status.getWasSuccessful()) {
            return Optional.of(email);
        }


        return Optional.empty();
    }

    @Override
    public boolean deleteCustomerEmail(String customerNumber, long id) throws IOException {
        DeleteEmail deleteEmail = new DeleteEmail(customerNumber, id);
        return deleteEmail.send(connection).getWasSuccessful();

    }

    private Optional<Email> mapMessageToEmail(Message message) throws IOException {
        if (message instanceof Retrieve retrieve) {
            Email email = new Email(
                    retrieve.getAddress(),
                    retrieve.getCustomerName(),
                    retrieve.getCustomerNumber(),
                    retrieve.getId(),
                    retrieve.getIsPrimary(),
                    retrieve.getOtherType(),
                    retrieve.getType()
            );
            return Optional.of(email);
        }
        return Optional.empty();
    }
}

