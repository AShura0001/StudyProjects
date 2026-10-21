package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.CustomerPayloads;
import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** A new customer is sent in several messages; a retry must carry on, never start again. */
class MainframeDraftUploaderResumeTest {

    /** Reports the connection as down once the given number of requests have been sent. */
    private static final class DroppingConnection extends ScriptedConnection {
        private final int upFor;

        DroppingConnection(int upFor) {
            this.upFor = upFor;
        }

        @Override
        public boolean isConnected() {
            return requests().size() < upFor;
        }
    }

    private static Draft customerWithOneAddress() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");
        customer.set_familyName("Doe");
        customer.set_title("Ms");
        customer.set_dateOfBirth("1990-01-01");
        customer.set_nationality("NZ");
        customer.setOrganisation(1L);
        Address address = new Address();
        address.setType("Residential");
        address.setLine1("1 Queen Street");
        address.setCountry("New Zealand");
        address.setIsMailing(true);
        address.setIsPrimary(true);
        return new Draft(DraftType.CUSTOMER,
                CustomerPayloads.customerPayload(customer, List.of(address), List.of(), List.of(), List.of()));
    }

    @Test
    void aConnectionLostAfterTheCustomerIsCreated_isResumedWithoutCreatingItAgain() {
        Draft draft = customerWithOneAddress();
        AtomicInteger saves = new AtomicInteger();

        DroppingConnection first = new DroppingConnection(1);
        first.replySuccess("number", "CUS-700");
        assertThrows(MainframeUnavailableException.class,
                () -> new MainframeDraftUploader(first).upload(draft, List.of(), saves::incrementAndGet));

        assertEquals("CUS-700", draft.checkpoint(MainframeDraftUploader.CUSTOMER_STEP).orElseThrow());
        assertEquals(1, saves.get(), "progress is saved as soon as the customer exists");

        ScriptedConnection second = new ScriptedConnection();
        second.replySuccess("id", "9", "customer", "CUS-700");
        String number = new MainframeDraftUploader(second).upload(draft, List.of(), saves::incrementAndGet);

        assertEquals("CUS-700", number);
        assertEquals(List.of("customer.addAddress"),
                second.requests().stream().map(MessageRequest::getRequestType).toList(),
                "the retry does not send customer.create again");
        assertEquals("CUS-700", second.requests().get(0).getValue("customer"));
        assertTrue(draft.checkpoint("address.0").isPresent());
    }

    @Test
    void aRefusedAddressAfterTheCustomerIsCreated_saysTheCustomerExists() {
        Draft draft = customerWithOneAddress();
        ScriptedConnection connection = new ScriptedConnection();
        connection.replySuccess("number", "CUS-701");
        connection.replyRefused("postcode must be a number");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> new MainframeDraftUploader(connection).upload(draft, List.of(), () -> { }));

        assertTrue(refused.getMessage().contains("Customer CUS-701 was created"), refused.getMessage());
        assertTrue(refused.getMessage().contains("address 1"), refused.getMessage());
        assertTrue(refused.getMessage().contains("postcode must be a number"), refused.getMessage());
        assertEquals("CUS-701", draft.checkpoint(MainframeDraftUploader.CUSTOMER_STEP).orElseThrow());
    }
}
