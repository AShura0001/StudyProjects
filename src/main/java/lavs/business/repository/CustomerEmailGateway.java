package lavs.business.repository;

import lavs.business.model.Address;
import lavs.business.model.Email;
import lavs.business.model.Employer;

import java.io.IOException;
import java.util.Optional;

public interface CustomerEmailGateway {


    /**
     *
     * @param id             Email id
     * @param customerNumber
     * @return An Optional wrap of an Email if it exists otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Email> findEmailByCustomerNumberAndId(Long id, String customerNumber) throws IOException;


    /**
     *
     * @param email An instance of the updated email
     * @return An Optional wrap of an Email if successful otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Email> updateCustomerEmail(Email email) throws IOException;


    /**
     *
     * @param customerNumber
     * @param id             Email id
     * @throws IOException
     */
    boolean deleteCustomerEmail(String customerNumber, long id) throws IOException;

    /**
     *
     * @param Email
     * @return
     * @throws IOException
     */
    Optional<Email> addCustomerEmail(Email email) throws IOException;
}
