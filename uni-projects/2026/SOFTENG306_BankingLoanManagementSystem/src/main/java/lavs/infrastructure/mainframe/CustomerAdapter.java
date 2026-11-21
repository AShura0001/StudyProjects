package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.Create;
import mainframe.client.messages.customer.Retrieve;
import mainframe.client.messages.customer.Search;
import mainframe.client.messages.customer.Update;
import mainframe.client.messages.customer.UpdateStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Talks to the mainframe for {@link lavs.business.repository.CustomerGateway}: it turns each call into the provided
 * customer messages, and their replies back into business objects.
 */
public class CustomerAdapter implements CustomerGateway {
    private final Connection connection;

    public CustomerAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Customer> findByCustomerNumber(String number) throws IOException {
        Retrieve retrieve = new Retrieve(number);
        if (!retrieve.send(connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return mapMessageToCustomer(retrieve);
    }

    @Override
    public boolean updateStatus(String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(customerNumber);
        if (!retrieve.send(connection).getWasSuccessful()) {
            return false;
        }
        // Turns an Active customer Inactive, and anything else (including a status the mainframe left out) Active.
        String status = UpdateStatus.STATUS_ACTIVE.equalsIgnoreCase(retrieve.getStatus())
                ? UpdateStatus.STATUS_INACTIVE : UpdateStatus.STATUS_ACTIVE;
        UpdateStatus updateStatus = new UpdateStatus(customerNumber, status);
        return updateStatus.send(connection).getWasSuccessful();
    }

    @Override
    public Optional<Customer> createCustomer(Customer customer) throws IOException {
        Create create = new Create(
                customer.get_dateOfBirth(),
                customer.get_familyName(),
                customer.get_firstName(),
                customer.get_nationality(),
                customer.getOrganisation(),
                customer.get_title()
        );
        if (create.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(create.getNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToCustomer(retrieve);
            }
            // The customer was created even though it could not be read back, so report it
            // with the number and details the mainframe sent when it created them.
            Customer created = new Customer(create.getDateOfBirth(), create.getFamilyName(), create.getFirstName(),
                    create.getNumber(), create.getStatus(), create.getTitle());
            created.set_nationality(create.getNationality());
            return Optional.of(created);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> updateCustomer(Customer customer) throws IOException {
        Update update = new Update(
                customer.get_dateOfBirth(),
                customer.get_familyName(),
                customer.get_firstName(),
                customer.get_nationality(),
                customer.get_number(),
                customer.get_title()
        );
        if (update.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(customer.get_number());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToCustomer(retrieve);
            }
            // The change was saved even though it could not be read back, so report the customer as sent.
            return Optional.of(customer);
        }
        return Optional.empty();
    }

    public CustomerSearchResult<Customer> searchCustomer(long page, String searchTerm, long page_size) throws IOException {
        Search search = new Search(page, searchTerm, page_size);
        if (search.send(connection).getWasSuccessful()) {
            ArrayList<Customer> customers = mapItemsToCustomer(search.getItems());
            return new CustomerSearchResult<>(customers, search.getTotal(), search.getPage());
        }
        return null;
    }

    private ArrayList<Customer> mapItemsToCustomer(Search.Item[] items) {
        ArrayList<Customer> customers = new ArrayList<>();
        for (Search.Item item : items) {
            Customer customer = new Customer(
                    item.getDateOfBirth(),
                    item.getFamilyName(),
                    item.getFirstName(),
                    item.getNumber(),
                    item.getStatus(),
                    item.getTitle()
                    );
            customers.add(customer);
        }
        return customers;

    }

    public Optional<Customer> mapMessageToCustomer(Message message) {
        if (message instanceof Retrieve retrieve) {
            Customer customer = new Customer();
            customer.set_dateOfBirth(retrieve.getDateOfBirth());
            customer.set_familyName(retrieve.getFamilyName());
            customer.set_firstName(retrieve.getFirstName());
            customer.set_nationality(retrieve.getNationality());
            customer.set_number(retrieve.getNumber());
            customer.set_status(retrieve.getStatus());
            customer.set_title(retrieve.getTitle());
            return Optional.of(customer);
        }
        return Optional.empty();
    }
}