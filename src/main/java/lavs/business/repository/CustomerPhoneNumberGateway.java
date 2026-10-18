package lavs.business.repository;

import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;

import java.io.IOException;
import java.util.Optional;

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
}
