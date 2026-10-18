package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.repository.CustomerAddressGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.AddAddress;
import mainframe.client.messages.customer.DeleteAddress;
import mainframe.client.messages.customer.UpdateAddress;
import mainframe.client.messages.customerAddress.List;
import mainframe.client.messages.customerAddress.Retrieve;

import java.io.IOException;
import java.util.ArrayList;
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
    public Optional<Address> updateCustomerAddress(Address address) throws IOException {
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

        if (updateAddress.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(address.getId(), address.getCustomerNumber());
            retrieve.send(connection);
            return mapMessageToAddress(retrieve);
        }
        ;

        return Optional.empty();
    }

    @Override
    public boolean deleteCustomerAddress(String customerNumber, long id) throws IOException {
        DeleteAddress deleteAddress = new DeleteAddress(customerNumber, id);
        return deleteAddress.send(connection).getWasSuccessful();
    }

    @Override
    public Optional<Address> addCustomerAddress(Address address) throws IOException {
        AddAddress addAddress = new AddAddress(
                address.getCity(),
                address.getCountry(),
                address.getCustomerNumber(),
                address.getIsMailing(),
                address.getIsPrimary(),
                address.getLine1(),
                address.getLine2(),
                address.getOtherType(),
                address.getPostCode(),
                address.getSuburb(),
                address.getType()
        );
        if (addAddress.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(address.getId(), addAddress.getCustomer());
            return mapMessageToAddress(retrieve);
        }
        return Optional.empty();
    }



    @Override
    public CustomerSearchResult<Address> ListCustomerAddresses(
            long page,
            String customerNumber,
            long pageSize
    ) throws IOException {

        mainframe.client.messages.customerAddress.List list =
                new mainframe.client.messages.customerAddress.List(
                        customerNumber,
                        page,
                        pageSize
                );

        if (!list.send(connection).getWasSuccessful()) {
            return new CustomerSearchResult<>(
                    java.util.List.of(),
                    0,
                    page
            );
        }

        ArrayList<Address> addresses =
                mapItemsToAddresslist(list);

        return new CustomerSearchResult<>(
                addresses,
                list.getTotal(),
                list.getPage()
        );
    }

    public ArrayList<Address> mapItemsToAddresslist(List list) {
        ArrayList<Address> addresses = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            Address address = new Address();
            address.setId(item.getId());
            address.setCity(item.getCity());
            address.setCountry(item.getCountry());
            address.setIsMailing(item.getIsMailing().equalsIgnoreCase("y") ? Boolean.TRUE : Boolean.FALSE);
            address.setIsPrimary(item.getIsPrimary().equalsIgnoreCase("y") ? Boolean.TRUE : Boolean.FALSE);
            address.setType(item.getType());
            address.setCustomerNumber(list.getCustomerNumber());
            address.setCustomerName(list.getCustomerName());

            addresses.add(address);

        }
        return addresses;
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
