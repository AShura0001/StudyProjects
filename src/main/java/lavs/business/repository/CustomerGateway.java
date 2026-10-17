package lavs.business.repository;

import lavs.business.model.Customer;

import java.io.IOException;
import java.util.Optional;

public interface CustomerGateway {
    Optional<Customer> findByCustomerNumber(String number) throws IOException;
}
