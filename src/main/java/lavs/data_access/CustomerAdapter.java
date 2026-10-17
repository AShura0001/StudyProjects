package lavs.data_access;

import lavs.business.model.Customer;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.Retrieve;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

public class CustomerAdapter implements CustomerGateway {
    private final Connection connection;


    public CustomerAdapter(Connection connection) {
        this.connection = connection;
    }

    /**
     *
     * @param number The customer number that uniquely identifies he/she/etc
     * @return Returns an Optional with the Customer class inside or an empty optional if the customer doesn't exist
     */
    public Optional<Customer> findByCustomerNumber(String number) throws IOException {
        Retrieve retrieve = new Retrieve(number);

        Status status = retrieve.send(connection);
        if (status.getWasSuccessful()) {

            // Map the response to the model class
            return mapMessageToCustomer(retrieve);
        }


        return Optional.empty();
    }

    /**
     *
     * @param message The message after the connection has been sent
     * @return a mapped Customer wrapped in an Optional
     */
    public Optional<Customer> mapMessageToCustomer(Message message) {
        if (message instanceof Retrieve) {

            Customer customer = new Customer();

            customer.set_dateOfBirth(((Retrieve) message).getDateOfBirth());
            customer.set_familyName(((Retrieve) message).getFamilyName());
            customer.set_firstName(((Retrieve) message).getFirstName());
            customer.set_nationality(((Retrieve) message).getNationality());
            customer.set_number(((Retrieve) message).getNumber());
            customer.set_status(((Retrieve) message).getStatus());
            customer.set_title(((Retrieve) message).getTitle());


            return Optional.of(customer);
        }

        return Optional.empty();
    }

}
