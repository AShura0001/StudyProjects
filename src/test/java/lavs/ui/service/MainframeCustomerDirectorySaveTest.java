package lavs.ui.service;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainframeCustomerDirectorySaveTest {

    private static final String NUMBER = "CUS-9";

    private RoutingConnection connection;
    private MainframeCustomerDirectory directory;

    @BeforeEach
    void setUp() {
        connection = new RoutingConnection();
        connection.reply("customer.retrieve", "number", NUMBER, "title", "Ms", "firstName", "Jane",
                "familyName", "Doe", "dateOfBirth", "1990-04-23", "nationality", "NZ", "status", "Active");
        connection.reply("customerPhoneNumber.list", "items", "1", "total", "1", "page", "1",
                "customerNumber", NUMBER, "1:id", "11", "1:type", "Mobile", "1:countryCode", "+64",
                "1:number", "21 555 0100", "1:isPrimary", "y", "1:canTxt", "y");
        connection.reply("customerPhoneNumber.retrieve", "id", "11", "customerNumber", NUMBER, "type", "Mobile",
                "countryCode", "+64", "number", "21 555 0100", "isPrimary", "y", "canTxt", "y");
        connection.reply("customerEmail.list", "items", "1", "total", "1", "page", "1",
                "customerNumber", NUMBER, "1:id", "12", "1:type", "Personal",
                "1:address", "jane@example.com", "1:isPrimary", "y");
        connection.reply("customerEmail.retrieve", "id", "12", "customerNumber", NUMBER, "type", "Personal",
                "address", "jane@example.com", "isPrimary", "y");
        connection.reply("customerAddress.list", "items", "1", "total", "1", "page", "1",
                "customerNumber", NUMBER, "1:id", "13", "1:type", "Residential", "1:city", "Auckland",
                "1:country", "New Zealand", "1:isPrimary", "y", "1:isMailing", "y");
        connection.reply("customerAddress.retrieve", "id", "13", "customerNumber", NUMBER, "type", "Residential",
                "line1", "1 Queen Street", "line2", "", "suburb", "CBD", "city", "Auckland",
                "postCode", "1010", "country", "New Zealand", "isPrimary", "y", "isMailing", "y");
        connection.reply("customerEmployer.list", "items", "2", "total", "2", "page", "1",
                "customerNumber", NUMBER, "1:id", "21", "1:name", "Acme", "2:id", "22", "2:name", "Globex");
        connection.reply("customerNote.list", "items", "0", "total", "0", "page", "1");
        connection.reply("loan.list", "items", "0", "total", "0", "page", "1");
        for (String write : List.of("customer.update", "customer.updatePhoneNumber", "customer.addPhoneNumber",
                "customer.updateEmail", "customer.updateAddress", "customer.deleteEmployer")) {
            connection.reply(write, "id", "11");
        }
        directory = new MainframeCustomerDirectory(connection);
    }

    private CustomerDirectory.Details unchanged() {
        return directory.details(NUMBER);
    }

    private static CustomerDirectory.Details with(CustomerDirectory.Details d, String firstName, String lastName,
                                                  String phone, String email, String line1, String line2,
                                                  String notes, List<CustomerDirectory.Employer> employers) {
        return new CustomerDirectory.Details(d.customerNumber(), firstName, lastName, phone, email, line1, line2,
                notes, employers, d.loans());
    }

    @Test
    void detailsShowsTheFullPrimaryAddress() {
        CustomerDirectory.Details details = unchanged();
        assertEquals("1 Queen Street", details.addressLine1());
        assertEquals("CBD, Auckland, 1010", details.addressLine2());
        assertEquals("+64 21 555 0100", details.phone());
    }

    @Test
    void nothingChangedSendsNoWrites() {
        CustomerDirectory.Details details = unchanged();
        connection.clearRequests();

        assertTrue(directory.save(details).isEmpty());
        assertTrue(connection.writes().isEmpty(), "unexpected writes: " + connection.writes());
    }

    @Test
    void changedFieldsAreSentAndTheRestKept() {
        CustomerDirectory.Details d = unchanged();
        connection.clearRequests();

        Map<String, String> errors = directory.save(with(d, "Janet", "Doe-Smith", "+61 400 111 222",
                "janet@example.com", "2 Queen Street", d.addressLine2(), d.notes(),
                List.of(new CustomerDirectory.Employer("Acme", "21"))));

        assertTrue(errors.isEmpty(), errors.toString());

        MessageRequest update = connection.sent("customer.update");
        assertEquals("Janet", update.getValue("firstName"));
        assertEquals("Doe-Smith", update.getValue("familyName"));
        assertEquals("Ms", update.getValue("title"));
        assertEquals("1990-04-23", update.getValue("dateOfBirth"));
        assertEquals("NZ", update.getValue("nationality"));

        MessageRequest phone = connection.sent("customer.updatePhoneNumber");
        assertEquals("+61", phone.getValue("countryCode"));
        assertEquals("400 111 222", phone.getValue("number"));
        assertEquals("11", phone.getValue("id"));
        assertEquals("y", phone.getValue("canTxt"));

        assertEquals("janet@example.com", connection.sent("customer.updateEmail").getValue("address"));

        MessageRequest address = connection.sent("customer.updateAddress");
        assertEquals("2 Queen Street", address.getValue("line1"));
        assertEquals("CBD", address.getValue("suburb"));
        assertEquals("1010", address.getValue("postCode"));

        assertEquals("22", connection.sent("customer.deleteEmployer").getValue("id"));
    }

    @Test
    void phoneWithoutCountryCodeKeepsTheCurrentCode() {
        CustomerDirectory.Details d = unchanged();

        directory.save(with(d, d.firstName(), d.lastName(), "21 999 0000", d.email(),
                d.addressLine1(), d.addressLine2(), d.notes(), d.employers()));

        MessageRequest phone = connection.sent("customer.updatePhoneNumber");
        assertEquals("+64", phone.getValue("countryCode"));
        assertEquals("21 999 0000", phone.getValue("number"));
    }

    @Test
    void invalidValuesAreReportedAndNothingIsSent() {
        CustomerDirectory.Details d = unchanged();
        connection.clearRequests();

        Map<String, String> errors = directory.save(with(d, "", "", "+64 abc", "not-an-email",
                d.addressLine1(), d.addressLine2(), d.notes(), d.employers()));

        assertTrue(errors.containsKey("firstName"));
        assertTrue(errors.containsKey("lastName"));
        assertTrue(errors.containsKey("phone"));
        assertTrue(errors.containsKey("email"));
        assertTrue(connection.writes().isEmpty(), "unexpected writes: " + connection.writes());
    }

    @Test
    void fieldsTheMainframeCannotTakeYetAreRefused() {
        CustomerDirectory.Details d = unchanged();
        connection.clearRequests();

        Map<String, String> errors = directory.save(with(d, d.firstName(), d.lastName(), d.phone(), d.email(),
                d.addressLine1(), "Ponsonby, Auckland", "Called about a top-up", d.employers()));

        assertTrue(errors.containsKey("addressLine2"));
        assertTrue(errors.containsKey("notes"));
        assertTrue(connection.writes().isEmpty());
    }

    @Test
    void newPhoneNeedsACountryCode() {
        connection.reply("customerPhoneNumber.list", "items", "0", "total", "0", "page", "1");
        CustomerDirectory.Details d = unchanged();

        Map<String, String> errors = directory.save(with(d, d.firstName(), d.lastName(), "021 555 0100",
                d.email(), d.addressLine1(), d.addressLine2(), d.notes(), d.employers()));
        assertTrue(errors.containsKey("phone"));

        assertTrue(directory.save(with(d, d.firstName(), d.lastName(), "+64 21 555 0100",
                d.email(), d.addressLine1(), d.addressLine2(), d.notes(), d.employers())).isEmpty());
        MessageRequest added = connection.sent("customer.addPhoneNumber");
        assertEquals("y", added.getValue("isPrimary"));
        assertEquals("Mobile", added.getValue("type"));
    }

    @Test
    void refusedWriteIsReported() {
        connection.refuse("customer.update");
        CustomerDirectory.Details d = unchanged();

        Map<String, String> errors = directory.save(with(d, "Janet", d.lastName(), d.phone(), d.email(),
                d.addressLine1(), d.addressLine2(), d.notes(), d.employers()));

        assertTrue(errors.get("form").contains("name"));
        assertFalse(connection.wasSent("customer.updatePhoneNumber"));
    }

    /** Answers each request type with a fixed reply, and records what was sent. */
    private static final class RoutingConnection implements Connection {
        private final Map<String, MessageResponse> replies = new HashMap<>();
        private final List<MessageRequest> requests = new ArrayList<>();

        void reply(String requestType, String... keyValues) {
            HashMap<String, String> values = new HashMap<>();
            for (int i = 0; i < keyValues.length; i += 2) {
                values.put(keyValues[i], keyValues[i + 1]);
            }
            replies.put(requestType, new MessageResponse(new Status(1), values));
        }

        void refuse(String requestType) {
            replies.put(requestType, new MessageResponse(
                    new Status(StatusMessage.SERVER_FAILURE.getCode(), "refused", 1), new HashMap<>()));
        }

        void clearRequests() {
            requests.clear();
        }

        List<String> writes() {
            return requests.stream().map(MessageRequest::getRequestType)
                    .filter(t -> !t.endsWith(".retrieve") && !t.endsWith(".list"))
                    .toList();
        }

        boolean wasSent(String requestType) {
            return requests.stream().anyMatch(r -> r.getRequestType().equals(requestType));
        }

        MessageRequest sent(String requestType) {
            return requests.stream().filter(r -> r.getRequestType().equals(requestType)).reduce((a, b) -> b)
                    .orElseThrow(() -> new AssertionError(requestType + " was not sent; sent " + writes()));
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            requests.add(request);
            MessageResponse reply = replies.get(request.getRequestType());
            if (reply == null) {
                throw new AssertionError("no reply set up for " + request.getRequestType());
            }
            return reply;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void close() {
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
