package lavs.business.repository;

import lavs.business.model.Document;
import lavs.business.model.Employer;
import lavs.business.model.Relationship;

import java.io.IOException;
import java.util.Optional;

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
}

