package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.CustomerPayloads;
import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.model.Email;
import lavs.business.model.Employer;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CUSTOMER branch of MainframeDraftUploader.upload(), added when
 * that branch changed from always refusing customer drafts to actually sending
 * them.
 *
 * NOTE for whoever merges this: an existing MainframeDraftUploaderTest already
 * covers the LOAN/COBORROWER branches and probably still asserts the OLD
 * behaviour for CUSTOMER (always refused, via the now-deleted
 * CUSTOMER_NOT_SUPPORTED_MESSAGE constant). That old assertion needs to be
 * removed or updated - this file only adds new tests for the new behaviour, it
 * does not touch the existing file. Please merge by hand, don't overwrite.
 *
 * These tests deliberately only assert on things verified from Create.java's
 * actual source (its request type "customer.create" and that it reads "number"
 * back from the response). The AddAddress/AddPhoneNumber/AddEmail/AddEmployer
 * message classes' own request/response wire format has not been seen directly
 * here - only their Java constructor signatures, inferred from the existing
 * adapters. So these tests check call counts and the overall outcome (customer
 * number returned, exceptions thrown correctly), not the exact wire contents
 * of each Add* call. If those message classes' source is available, it would
 * be worth strengthening these tests to assert their request types and fields
 * directly, the way the Create-based tests below do.
 */
class MainframeDraftUploaderCustomerTest {

    @Test
    void upload_aCustomerDraftWithNoChildRecords_createsOnlyTheCustomer() {
        RecordingConnection connection = new RecordingConnection();
        connection.nextResponses.add(successResponse(Map("number", "CUS-500", "familyName", "Doe",
                "firstName", "Jane", "title", "Ms", "dateOfBirth", "1990-01-01", "nationality", "NZ")));
        MainframeDraftUploader uploader = new MainframeDraftUploader(connection);
        Draft draft = customerDraft(someCustomer(), List.of(), List.of(), List.of(), List.of());

        String result = uploader.upload(draft, List.of());

        assertEquals("CUS-500", result);
        assertEquals(1, connection.requests.size());
        assertEquals("customer.create", connection.requests.get(0).getRequestType());
    }

    @Test
    void upload_aCustomerDraftWithChildRecords_sendsOneRequestPerRecordAfterTheCustomer() {
        RecordingConnection connection = new RecordingConnection();
        connection.nextResponses.add(successResponse(Map("number", "CUS-501"))); // customer.create
        connection.nextResponses.add(successResponse(Map("id", "101")));          // customer.addAddress
        connection.nextResponses.add(successResponse(Map("id", "102")));          // customer.addPhoneNumber
        connection.nextResponses.add(successResponse(Map("id", "103")));          // customer.addEmail
        connection.nextResponses.add(successResponse(Map("id", "104", "salary", "75000.0"))); // customer.addEmployer

        MainframeDraftUploader uploader = new MainframeDraftUploader(connection);
        Draft draft = customerDraft(someCustomer(),
                List.of(someAddress()), List.of(somePhone()), List.of(someEmail()), List.of(someEmployer()));

        String result = uploader.upload(draft, List.of());

        assertEquals("CUS-501", result);
        assertEquals(5, connection.requests.size());
        assertEquals("customer.create", connection.requests.get(0).getRequestType());
    }

    @Test
    void upload_whenTheCustomerCreateIsRefused_throwsRecordRejectedAndSendsNoChildRecords() {
        RecordingConnection connection = new RecordingConnection();
        connection.nextResponses.add(failedResponse(3, "The mainframe could not process the message"));
        MainframeDraftUploader uploader = new MainframeDraftUploader(connection);
        Draft draft = customerDraft(someCustomer(), List.of(someAddress()), List.of(), List.of(), List.of());

        assertThrows(RecordRejectedException.class, () -> uploader.upload(draft, List.of()));

        assertEquals(1, connection.requests.size()); // the address was never attempted
    }

    @Test
    void upload_whenTheMainframeCannotBeReached_throwsMainframeUnavailableAndSendsNothing() {
        RecordingConnection connection = new RecordingConnection();
        connection.connected = false;
        MainframeDraftUploader uploader = new MainframeDraftUploader(connection);
        Draft draft = customerDraft(someCustomer(), List.of(), List.of(), List.of(), List.of());

        assertThrows(MainframeUnavailableException.class, () -> uploader.upload(draft, List.of()));

        assertTrue(connection.requests.isEmpty());
    }

    @Test
    void upload_aCustomerDraftWithAnUnreadablePayload_isRefusedWithoutContactingTheMainframe() {
        RecordingConnection connection = new RecordingConnection();
        MainframeDraftUploader uploader = new MainframeDraftUploader(connection);
        Draft draft = new Draft(DraftType.CUSTOMER, "this is not valid customer json");

        RecordRejectedException thrown =
                assertThrows(RecordRejectedException.class, () -> uploader.upload(draft, List.of()));

        assertEquals(MainframeDraftUploader.UNREADABLE_DRAFT_MESSAGE, thrown.getMessage());
        assertTrue(connection.requests.isEmpty());
    }

    // ---- Helpers ----

    private static Draft customerDraft(Customer customer, List<Address> addresses, List<PhoneNumber> phones,
                                        List<Email> emails, List<Employer> employers) {
        String payload = CustomerPayloads.customerPayload(customer, addresses, phones, emails, employers);
        return new Draft(DraftType.CUSTOMER, payload);
    }

    private static Customer someCustomer() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");
        customer.set_familyName("Doe");
        customer.set_title("Ms");
        customer.set_dateOfBirth("1990-01-01");
        customer.set_nationality("NZ");
        customer.setOrganisation(1L);
        return customer;
    }

    private static Address someAddress() {
        Address address = new Address();
        address.setType("Home");
        address.setLine1("1 Queen Street");
        address.setCountry("New Zealand");
        address.setIsMailing(true);
        address.setIsPrimary(true);
        return address;
    }

    private static PhoneNumber somePhone() {
        PhoneNumber phone = new PhoneNumber(true, "64", null, null, 0L, true, null, "Mobile");
        phone.set_number("211234567");
        return phone;
    }

    private static Email someEmail() {
        return new Email("jane@example.com", null, null, 0L, false, null, "Personal");
    }

    private static Employer someEmployer() {
        return new Employer(null, null, 0L, "Acme Ltd", "Developer", 75000.0, "Full-time");
    }

    private static MessageResponse successResponse(HashMap<String, String> values) {
        return new MessageResponse(new Status(1), values);
    }

    private static MessageResponse failedResponse(int errorCode, String errorMessage) {
        return new MessageResponse(new Status(errorCode, errorMessage, 1), new HashMap<>());
    }

    private static HashMap<String, String> Map(String... keyValuePairs) {
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    /** Records every request sent and returns the next scripted response in order. */
    private static final class RecordingConnection implements Connection {
        private final List<MessageRequest> requests = new ArrayList<>();
        private final List<MessageResponse> nextResponses = new ArrayList<>();
        private boolean connected = true;
        private int callIndex = 0;

        @Override
        public void close() {
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            requests.add(request);
            if (callIndex < nextResponses.size()) {
                return nextResponses.get(callIndex++);
            }
            // Default: succeed with a generic "number" in case more calls happen than scripted.
            return successResponse(Map("number", "CUS-DEFAULT"));
        }

        @Override
        public Status Login(String username, String password) {
            return new Status(1);
        }

        @Override
        public void Logout() {
        }
    }
}
