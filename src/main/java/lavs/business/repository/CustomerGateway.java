package lavs.business.repository;

import lavs.business.model.Customer;

import java.io.IOException;
import java.util.Optional;

public interface CustomerGateway {

    /**
     *
     * @param number customer number
     * @return An Optional wrap of a Customer if it exists otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Customer> findByCustomerNumber(String number) throws IOException;
}
