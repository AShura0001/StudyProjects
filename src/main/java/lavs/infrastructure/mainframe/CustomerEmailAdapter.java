package lavs.infrastructure.mainframe;

import lavs.business.model.Email;
import lavs.business.repository.CustomerEmailGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.AddEmail;
import mainframe.client.messages.customer.DeleteEmail;
import mainframe.client.messages.customer.UpdateEmail;
import mainframe.client.messages.customerEmail.List;
import mainframe.client.messages.customerEmail.Retrieve;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerEmailAdapter implements CustomerEmailGateway {
    private final Connection connection;

    public CustomerEmailAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Email> findEmailByCustomerNumberAndId(Long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        if (!retrieve.send(connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return mapMessageToEmail(retrieve);
    }


    @Override
    public Optional<Email> updateCustomerEmail(Email email) throws IOException {
        UpdateEmail updateEmail = new UpdateEmail(
                email.getAddress(),
                email.getCustomerNumber(),
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

    @Override
    public Optional<Email> addCustomerEmail(Email email) throws IOException {
        AddEmail addEmail = new AddEmail(
                email.getAddress(),
                email.getCustomerNumber(),
                email.isPrimary(),
                email.getOtherType(),
                email.getType()
        );
        if(addEmail.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(addEmail.getId(), email.getCustomerNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToEmail(retrieve);
            }
        }
        return Optional.empty();
    }

    @Override
    public CustomerSearchResult<Email> ListCustomerEmails(long page, String customerNumber, long page_size) throws IOException {
        List list = new List(customerNumber, page, page_size);
        if (!list.send(connection).getWasSuccessful()) {
            return new CustomerSearchResult<>(
                    java.util.List.of(),
                    0,
                    page
            );
        }

        return new CustomerSearchResult<>(
                mapItemsToEmailList(list),
                list.getTotal(),
                list.getPage()
        );
    }

    private ArrayList<Email> mapItemsToEmailList(List list) {
        ArrayList<Email> items = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            Email email = new Email();
            email.setId(item.getId());
            email.setAddress(item.getAddress());
            email.setPrimary("y".equalsIgnoreCase(item.getIsPrimary()));
            email.setType(item.getType());
            email.setCustomerNumber(list.getCustomerNumber());
            email.setCustomerName(list.getCustomerName());
            items.add(email);
        }
        return items;
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

