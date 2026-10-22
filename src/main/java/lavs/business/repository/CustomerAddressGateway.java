package lavs.business.repository;

import lavs.business.model.Address;

import java.io.IOException;
import java.util.Optional;

/**
 * What the business layer needs from the mainframe for a customer's addresses. The data access
 * layer implements it, so the services can be tested with a stand-in instead of the mainframe.
 * Every method throws {@link java.io.IOException} if the mainframe could not be reached.
 */
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
    boolean deleteCustomerAddress(String customerNumber, long id) throws IOException;

    /**
     *
     * @param address
     * @return
     * @throws IOException
     */
    Optional<Address> addCustomerAddress(Address address) throws IOException;

    /**
     *
     * @param page  The current page of Customers to retrieve
     * @param customerNumber String search term
     * @param page_size Number of elements per page
     * @return
     * @throws IOException
     */
    CustomerSearchResult<Address> ListCustomerAddresses(long page, String customerNumber, long page_size) throws IOException;


}
