package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import lavs.business.repository.CustomerGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.Retrieve;

import java.io.IOException;
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
