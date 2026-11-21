package lavs.business.repository;

import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;

import java.io.IOException;
import java.util.Optional;

/**
 * What the business layer needs from the mainframe for a customer's phone numbers. The data access
 * layer implements it, so the services can be tested with a stand-in instead of the mainframe.
 * Every method throws {@link java.io.IOException} if the mainframe could not be reached.
 */
public interface CustomerPhoneNumberGateway {

    /**
     *
     * @param id Number id
     * @param customerNumber
     * @return Returns a number wrapped in Optional if it exists
     */
    public Optional<PhoneNumber> findCustomerNumberByNumberAndId(long id, String customerNumber) throws IOException;

    /**
     *
     * @param phoneNumber
     * @return an Optional wrap of the updated phone number if successful, an Optional wrap of empty if not successful
     */
    public Optional<PhoneNumber> updateCustomerNumber(PhoneNumber phoneNumber) throws IOException;

    /**
     *
     * @param id
     * @param customerNumber
     * @return true if successfully deleted
     */
    boolean deleteCustomerNumberByNumberAndId(long id, String customerNumber) throws IOException;

    /**
     *
     * @param phoneNumber
     * @return
     * @throws IOException
     */
    Optional<PhoneNumber> addCustomerPhoneNumber(PhoneNumber phoneNumber) throws IOException;

    /**
     *
     * @param page  The current page of PhoneNumbers to retrieve
     * @param customerNumber The customer whose PhoneNumbers are listed
     * @param page_size Number of elements per page
     * @return A page of PhoneNumbers for the customer, empty if the request failed
     * @throws IOException
     */
    CustomerSearchResult<PhoneNumber> ListCustomerPhoneNumbers(long page, String customerNumber, long page_size) throws IOException;
}
