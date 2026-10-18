package lavs.business.repository;

import lavs.business.model.Customer;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CustomerGateway {

    /**
     *
     * @param number customer number
     * @return An Optional wrap of a Customer if it exists otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Customer> findByCustomerNumber(String number) throws IOException;

    /**
     *
     * @param customerNumber
     * @return toggles the status between active and inactive and returns true if successful
     * @throws IOException
     */
    boolean updateStatus(String customerNumber) throws IOException;


    /**
     *
     * @param customer an instance of a customer object
     * @return  Retrieves the customer created from the mainframe
     * @throws IOException
     */
    Optional<Customer> createCustomer(Customer customer) throws IOException;

    /**
     *
     * @param page  The current page of Customers to retrieve
     * @param searchTerm String search term
     * @param page_size Number of elements per page
     * @return
     * @throws IOException
     */
    CustomerSearchResult searchCustomer(long page, String searchTerm, long page_size) throws IOException;


}
