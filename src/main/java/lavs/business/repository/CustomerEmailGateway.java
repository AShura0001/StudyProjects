package lavs.business.repository;

import lavs.business.model.Address;
import lavs.business.model.Email;

import java.io.IOException;
import java.util.Optional;

public interface CustomerEmailGateway {

    Optional<Email> findEmailByCustomerNumberAndId(Long id, String customerNumber) throws IOException;

    Optional<Email> updateCustomerEmail(Email email) throws IOException;

    void deleteCustomerEmail(String customerNumber, long id) throws IOException;
}
