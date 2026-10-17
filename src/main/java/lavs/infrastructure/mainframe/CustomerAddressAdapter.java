package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.repository.CustomerAddressGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.DeleteAddress;
import mainframe.client.messages.customer.UpdateAddress;
import mainframe.client.messages.customerAddress.Retrieve;

import java.io.IOException;
import java.util.Optional;

public class CustomerAddressAdapter implements CustomerAddressGateway {
    private final Connection connection;

    public CustomerAddressAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Address> findAddressByCustomerNumberAndId(Long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        if (!retrieve.send(connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return mapMessageToAddress(retrieve);
    }

    @Override
    public Address updateCustomerAddress(Address address) throws IOException {
        UpdateAddress updateAddress = new UpdateAddress(
                address.getCity(),
                address.getCountry(),
                address.getCustomerNumber(),
                address.getId(),
                Boolean.TRUE.equals(address.getIsMailing()),
                Boolean.TRUE.equals(address.getIsPrimary()),
                address.getLine1(),
                address.getLine2(),
                address.getOtherType(),
                address.getPostCode(),
                address.getSuburb(),
                address.getType());

        updateAddress.send(connection);
        return mapMessageToAddress(updateAddress).orElse(address);
    }

    @Override
    public void deleteCustomerAddress(String customerNumber, long id) throws IOException {
        DeleteAddress deleteAddress = new DeleteAddress(customerNumber, id);
        deleteAddress.send(connection);
    }

    public Optional<Address> mapMessageToAddress(Message message) {
        Address address = new Address();

        if (message instanceof Retrieve retrieve) {
            address.setCity(retrieve.getCity());
            address.setCountry(retrieve.getCountry());
            address.setCustomerName(retrieve.getCustomerName());
            address.setCustomerNumber(retrieve.getCustomerNumber());
            address.setId(retrieve.getId());
            address.setIsMailing(retrieve.getIsMailing());
            address.setIsPrimary(retrieve.getIsPrimary());
            address.setLine1(retrieve.getLine1());
            address.setLine2(retrieve.getLine2());
            address.setOtherType(retrieve.getOtherType());
            address.setPostCode(retrieve.getPostCode());
            address.setSuburb(retrieve.getSuburb());
            address.setType(retrieve.getType());
            return Optional.of(address);
        }

        if (message instanceof UpdateAddress updateAddress) {
            address.setCity(updateAddress.getCity());
            address.setCountry(updateAddress.getCountry());
            address.setCustomerNumber(updateAddress.getCustomer());
            address.setId(updateAddress.getId());
            address.setIsMailing(updateAddress.getIsMailing());
            address.setIsPrimary(updateAddress.getIsPrimary());
            address.setLine1(updateAddress.getLine1());
            address.setLine2(updateAddress.getLine2());
            address.setOtherType(updateAddress.getOtherType());
            address.setPostCode(updateAddress.getPostCode());
            address.setSuburb(updateAddress.getSuburb());
            address.setType(updateAddress.getType());
            return Optional.of(address);
        }

        return Optional.empty();
    }
}
