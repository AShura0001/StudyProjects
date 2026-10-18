package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.Create;
import mainframe.client.messages.customer.Retrieve;
import mainframe.client.messages.customer.Search;
import mainframe.client.messages.customer.UpdateStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

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
        String status = retrieve.getStatus().equalsIgnoreCase(UpdateStatus.STATUS_ACTIVE) ? UpdateStatus.STATUS_INACTIVE : UpdateStatus.STATUS_ACTIVE;
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
            retrieve.send(connection);
            return mapMessageToCustomer(retrieve);
        }
        return Optional.empty();
    }

    public CustomerSearchResult searchCustomer(long page, String searchTerm, long page_size) throws IOException {
        Search search = new Search(page, searchTerm, page_size);
        if (search.send(connection).getWasSuccessful()) {
            ArrayList<Customer> customers = mapItemsToCustomer(search.getItems());
            return new CustomerSearchResult(customers, search.getTotal(), search.getPage());
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
