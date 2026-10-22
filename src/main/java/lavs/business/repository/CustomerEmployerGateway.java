package lavs.business.repository;

import lavs.business.model.Document;
import lavs.business.model.Employer;
import lavs.business.model.Relationship;

import java.io.IOException;
import java.util.Optional;

/**
 * What the business layer needs from the mainframe for a customer's employers. The data access
 * layer implements it, so the services can be tested with a stand-in instead of the mainframe.
 * Every method throws {@link java.io.IOException} if the mainframe could not be reached.
 */
public interface CustomerEmployerGateway {

    /**
     *
     * @param id
     * @param customerNumber
     * @return An Optional wrap of the Employer if it exists, otherwise an Optional.empty()
     */
    Optional<Employer> findEmployerByCustomerNumberAndId(long id, String customerNumber) throws IOException;

    /**
     * This method only updates Employer individually
     * @param employer and instance of the Employer
     * @return An Optional wrap of the Employer when retrieved, an Optional.empty() when otherwise
     */
    Optional<Employer> updateCustomerEmployer(Employer employer) throws IOException;

    /**
     *
     * @param id
     * @param customerNumber
     * @return true if successful
     */
    boolean deleteEmployerByCustomerNumberAndId(long id, String customerNumber) throws IOException;

    /**
     *
     * @param employer
     * @return
     * @throws IOException
     */
    Optional<Employer> addCustomerEmployer(Employer employer) throws IOException;

    /**
     *
     * @param page  The current page of Employers to retrieve
     * @param customerNumber The customer whose Employers are listed
     * @param page_size Number of elements per page
     * @return A page of Employers for the customer, empty if the request failed
     * @throws IOException
     */
    CustomerSearchResult<Employer> ListCustomerEmployers(long page, String customerNumber, long page_size) throws IOException;
}
