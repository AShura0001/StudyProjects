package lavs.data_access;

import lavs.business.model.Address;
import lavs.business.model.Customer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public interface CustomerGateway {

    public Optional<Customer> findByCustomerNumber(String number) throws IOException;

    public Optional<Address> updateCustomerAddress(Address address) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException;

}
