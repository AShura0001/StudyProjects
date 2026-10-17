package lavs.data_access;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.data_access.exceptions.InvalidAddressMissingCustomerNumber;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.AddAddress;
import mainframe.client.messages.customer.Retrieve;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
     * @param address An instance of the address you want to update, it should contain the customer number in the class
     * @return  The address that was updated if successful wrapped in an Optional,
     * @throws IOException
     */
    public Optional<Address> updateCustomerAddress(Address address) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if(address.get_customerNumber() == null){
            throw new InvalidAddressMissingCustomerNumber("Missing Customer Number");
        }

        AddAddress addAddress = new AddAddress(
                address.get_city(),
                address.get_country(),
                address.get_customerNumber(),
                address.is_isMailing(),
                address.is_isPrimary(),
                address.get_line1(),
                address.get_line2(),
                address.get_otherType(),
                address.get_postCode(),
                address.get_suburb(),
                address.get_type()
        );
        Status status = addAddress.send(connection);

        // After a successful update it tries to retrieve the message it just updated
        if (status.getWasSuccessful()) {
            long addAddressId = addAddress.getId();
            mainframe.client.messages.customerAddress.Retrieve retrieveAddress =
                    new mainframe.client.messages.customerAddress.Retrieve(addAddressId, address.get_customerNumber());
            retrieveAddress.send(connection);

            // Map the response to the model class
            return mapMessageToAddress(retrieveAddress);
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


    /**
     *
     * @param message customerAddress.Retrieve Message
     * @return An Address object
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Optional<Address> mapMessageToAddress(Message message) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (message instanceof mainframe.client.messages.customerAddress.Retrieve) {

            Address address = new Address(
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getCity(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getCountry(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getCustomerName(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getCustomerNumber(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getId(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getIsMailing(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getIsPrimary(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getLine1(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getLine2(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getOtherType(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getPostCode(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getSuburb(),
                    ((mainframe.client.messages.customerAddress.Retrieve) message).getType()
            );
            return Optional.of(address);

        }

        return Optional.empty();
    }


}
