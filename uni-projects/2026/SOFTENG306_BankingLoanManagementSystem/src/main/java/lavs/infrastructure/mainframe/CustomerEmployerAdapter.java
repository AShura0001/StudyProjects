package lavs.infrastructure.mainframe;

import lavs.business.model.Employer;
import lavs.business.repository.CustomerEmployerGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.messages.customer.AddEmployer;
import mainframe.client.messages.customer.DeleteEmployer;
import mainframe.client.messages.customer.UpdateEmployer;
import mainframe.client.messages.customerEmployer.List;
import mainframe.client.messages.customerEmployer.Retrieve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Talks to the mainframe for {@link lavs.business.repository.CustomerEmployerGateway}: it turns each call into the provided
 * customer employer messages, and their replies back into business objects.
 */
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
                employer.get_customerNumber(),
                employer.get_id(),
                employer.get_name(),
                employer.get_position(),
                employer.get_salary(),
                employer.get_type()
        );
        if (updateEmployer.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(employer.get_id(), employer.get_customerNumber());
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
            Retrieve retrieve = new Retrieve(addEmployer.getId(), employer.get_customerNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToEmployer(retrieve);
            }
        }


        return Optional.empty();
    }

    @Override
    public CustomerSearchResult<Employer> ListCustomerEmployers(long page, String customerNumber, long page_size) throws IOException {
        List list = new List(customerNumber, page, page_size);
        if (!list.send(connection).getWasSuccessful()) {
            return new CustomerSearchResult<>(
                    java.util.List.of(),
                    0,
                    page
            );
        }

        return new CustomerSearchResult<>(
                mapItemsToEmployerList(list),
                list.getTotal(),
                list.getPage()
        );
    }

    private ArrayList<Employer> mapItemsToEmployerList(List list) {
        ArrayList<Employer> items = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            Employer employer = new Employer();
            employer.set_id(item.getId());
            employer.set_name(item.getName());
            employer.set_type(item.getType());
            employer.set_customerNumber(list.getCustomerNumber());
            employer.set_customerName(list.getCustomerName());
            items.add(employer);
        }
        return items;
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
