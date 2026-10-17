package lavs.business.repository;

import lavs.business.model.Address;

import java.io.IOException;
import java.util.Optional;

public interface CustomerAddressGateway {
    Optional<Address> findAddressByCustomerNumberAndId(Long id, String customerNumber) throws IOException;

    Optional<Address> updateCustomerAddress(Address address) throws IOException;

    void deleteCustomerAddress(String customerNumber, long id) throws IOException;
}
