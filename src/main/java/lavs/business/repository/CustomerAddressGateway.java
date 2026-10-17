package lavs.business.repository;

import lavs.business.model.Address;

import java.io.IOException;
import java.util.Optional;

public interface CustomerAddressGateway {

    /**
     *
     * @param id Address id
     * @param customerNumber
     * @return An Optional wrap of an Address if it exists otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Address> findAddressByCustomerNumberAndId(Long id, String customerNumber) throws IOException;


    /**
     *
     * @param address An instance of the updated address
     * @return  An Optional wrap of an Address if it successful otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Address> updateCustomerAddress(Address address) throws IOException;


    /**
     *
     * @param customerNumber
     * @param id Address id
     * @throws IOException
     */
    void deleteCustomerAddress(String customerNumber, long id) throws IOException;
}
