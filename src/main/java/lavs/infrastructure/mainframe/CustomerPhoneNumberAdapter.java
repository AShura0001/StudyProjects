package lavs.infrastructure.mainframe;

import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerPhoneNumberGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.DeletePhoneNumber;
import mainframe.client.messages.customer.UpdatePhoneNumber;
import mainframe.client.messages.customerPhoneNumber.Retrieve;

import java.io.IOException;
import java.util.Optional;

public class CustomerPhoneNumberAdapter implements CustomerPhoneNumberGateway {
    private final Connection connection;

    public CustomerPhoneNumberAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<PhoneNumber> findCustomerNumberByNumberAndId(long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        Status status = retrieve.send(connection);
        if (status.getWasSuccessful()) {
            return mapMessageToPhoneNumber(retrieve);
        }

        return Optional.empty();
    }

    @Override
    public Optional<PhoneNumber> updateCustomerNumber(PhoneNumber phoneNumber) throws IOException {
        UpdatePhoneNumber updatePhoneNumber = new UpdatePhoneNumber(
                phoneNumber.is_canTxt(),
                phoneNumber.get_countryCode(),
                phoneNumber.get_customerNumber(),
                phoneNumber.get_id(),
                phoneNumber.is_isPrimary(),
                phoneNumber.get_number(),
                phoneNumber.get_otherType(),
                phoneNumber.get_type()
        );
        if(updatePhoneNumber.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(phoneNumber.get_id(), phoneNumber.get_number());
            Status status = retrieve.send(connection);
            if (status.getWasSuccessful()) {
                return mapMessageToPhoneNumber(retrieve);
            }
        };
        return Optional.empty();
    }

    @Override
    public boolean deleteCustomerNumberByNumberAndId(long id, String customerNumber) throws IOException {
        DeletePhoneNumber deletePhoneNumber = new DeletePhoneNumber(customerNumber, id);
        return deletePhoneNumber.send(connection).getWasSuccessful();
    }

    public Optional<PhoneNumber> mapMessageToPhoneNumber(Message message) {
        if (message instanceof Retrieve retrieve) {
            return Optional.of(new PhoneNumber(
                    retrieve.getCanTxt(),
                    retrieve.getCountryCode(),
                    retrieve.getCustomerName(),
                    retrieve.getCustomerNumber(),
                    retrieve.getId(),
                    retrieve.getIsPrimary(),
                    retrieve.getOtherType(),
                    retrieve.getType()
            ));
        }

        return Optional.empty();
    }
}
