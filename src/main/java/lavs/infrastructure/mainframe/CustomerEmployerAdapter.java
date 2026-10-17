package lavs.infrastructure.mainframe;

import lavs.business.model.Employer;
import lavs.business.repository.CustomerEmployerGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.AddEmployer;
import mainframe.client.messages.customer.DeleteEmployer;
import mainframe.client.messages.customer.UpdateEmployer;
import mainframe.client.messages.customerEmployer.Retrieve;

import java.io.IOException;
import java.util.Optional;

public class CustomerEmployerAdapter implements CustomerEmployerGateway {
    private final Connection connection;

    public CustomerEmployerAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Employer> findEmployerByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        if (retrieve.send(connection).getWasSuccessful()) {
            return mapMessageToEmployer(retrieve);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Employer> updateCustomerEmployer(Employer employer) throws IOException {
        UpdateEmployer updateEmployer = new UpdateEmployer(
                employer.get_customerName(),
                employer.get_id(),
                employer.get_name(),
                employer.get_position(),
                employer.get_salary(),
                employer.get_type()
        );
        if (updateEmployer.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(employer.get_id(), employer.get_customerName());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToEmployer(retrieve);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean deleteEmployerByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        DeleteEmployer deleteEmployer = new DeleteEmployer(customerNumber, id);
        return deleteEmployer.send(connection).getWasSuccessful();
    }

    @Override
    public Optional<Employer> addCustomerEmployer(Employer employer) throws IOException {
        AddEmployer addEmployer = new AddEmployer(
                employer.get_customerNumber(),
                employer.get_name(),
                employer.get_position(),
                employer.get_salary(),
                employer.get_type()
        );
        if (addEmployer.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(addEmployer.getId(), addEmployer.getCustomer());
            return mapMessageToEmployer(retrieve);
        }


        return Optional.empty();
    }

    public Optional<Employer> mapMessageToEmployer(Message message) throws IOException {
        if (message instanceof Retrieve retrieve) {
            return Optional.of(new Employer(
                    retrieve.getCustomerName(),
                    retrieve.getCustomerNumber(),
                    retrieve.getId(),
                    retrieve.getName(),
                    retrieve.getPosition(),
                    retrieve.getSalary(),
                    retrieve.getType()
            ));
        }
        return Optional.empty();
    }
}
