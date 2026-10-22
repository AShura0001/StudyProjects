package lavs.infrastructure.mainframe;

import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The phone number adapter against a scripted connection, so no mainframe is needed. */
class CustomerPhoneNumberAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerPhoneNumberAdapter adapter = new CustomerPhoneNumberAdapter(connection);

    private static PhoneNumber mobile(long id) {
        PhoneNumber phone = new PhoneNumber(true, "+64", "Jane Doe", "CUS-1", id, true, null, "Mobile");
        phone.set_number("21 555 0100");
        return phone;
    }

    private void retrieveReply() {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "id", "11", "type", "Mobile",
                "otherType", "", "countryCode", "+64", "number", "21 555 0100", "isPrimary", "y", "canTxt", "n");
    }

    @Test
    void find_readsEveryFieldOfThePhone() throws Exception {
        retrieveReply();

        PhoneNumber phone = adapter.findCustomerNumberByNumberAndId(11, "CUS-1").orElseThrow();

        assertEquals("21 555 0100", phone.get_number());
        assertEquals("+64", phone.get_countryCode());
        assertEquals("Mobile", phone.get_type());
        assertEquals(11, phone.get_id());
        assertTrue(phone.is_isPrimary());
        assertFalse(phone.is_canTxt());
        assertEquals("Jane Doe", phone.get_customerName());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("customerPhoneNumber.retrieve", sent.getRequestType());
        assertEquals("11", sent.getValue("id"));
        assertEquals("CUS-1", sent.getValue("number"));
    }

    @Test
    void find_whenThereIsNone_isEmpty() throws Exception {
        connection.replyRefused("No such phone number.");

        assertTrue(adapter.findCustomerNumberByNumberAndId(11, "CUS-1").isEmpty());
    }

    @Test
    void add_sendsThePhoneThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "11", "type", "Mobile", "number", "21 555 0100",
                "countryCode", "+64", "isPrimary", "y", "canTxt", "y");
        retrieveReply();

        Optional<PhoneNumber> saved = adapter.addCustomerPhoneNumber(mobile(0));

        assertEquals(11, saved.orElseThrow().get_id());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addPhoneNumber", add.getRequestType());
        assertEquals("CUS-1", add.getValue("customer"));
        assertEquals("21 555 0100", add.getValue("number"));
        assertEquals("y", add.getValue("canTxt"));
        assertEquals("y", add.getValue("isPrimary"));
        assertEquals("11", connection.requests().get(1).getValue("id"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("Bad number.");

        assertTrue(adapter.addCustomerPhoneNumber(mobile(0)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "11");
        connection.replyRefused("No such phone number.");

        assertTrue(adapter.addCustomerPhoneNumber(mobile(0)).isEmpty());
    }

    @Test
    void update_sendsTheChangeThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "11");
        retrieveReply();

        assertTrue(adapter.updateCustomerNumber(mobile(11)).isPresent());

        MessageRequest update = connection.requests().get(0);
        assertEquals("customer.updatePhoneNumber", update.getRequestType());
        assertEquals("11", update.getValue("id"));
    }

    @Test
    void update_whenTheMainframeRefuses_givesNothing() throws Exception {
        connection.replyRefused("Not allowed.");

        assertTrue(adapter.updateCustomerNumber(mobile(11)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void update_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "11");
        connection.replyRefused("No such phone number.");

        assertTrue(adapter.updateCustomerNumber(mobile(11)).isEmpty());
    }

    @Test
    void delete_isTrueOnlyWhenTheMainframeAccepts() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "11");
        connection.replyRefused("No such phone number.");

        assertTrue(adapter.deleteCustomerNumberByNumberAndId(11, "CUS-1"));
        assertFalse(adapter.deleteCustomerNumberByNumberAndId(11, "CUS-1"));
        assertEquals("customer.deletePhoneNumber", connection.requests().get(0).getRequestType());
    }

    @Test
    void list_readsEachPhoneAndTheTotal() throws Exception {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "11", "1:type", "Mobile", "1:countryCode", "+64", "1:number", "21 1", "1:isPrimary", "y",
                "1:canTxt", "y",
                "2:id", "12", "2:type", "Home", "2:countryCode", "+64", "2:number", "9 2", "2:isPrimary", "n",
                "2:canTxt", "n");

        CustomerSearchResult<PhoneNumber> result = adapter.ListCustomerPhoneNumbers(1, "CUS-1", 100);

        assertEquals(2, result.getTotal());
        List<PhoneNumber> phones = result.getList();
        assertTrue(phones.get(0).is_isPrimary());
        assertTrue(phones.get(0).is_canTxt());
        assertFalse(phones.get(1).is_isPrimary());
        assertEquals("9 2", phones.get(1).get_number());
        assertEquals("CUS-1", phones.get(1).get_customerNumber());
    }

    @Test
    void list_whenTheMainframeRefuses_isEmpty() throws Exception {
        connection.replyRefused("No such customer.");

        CustomerSearchResult<PhoneNumber> result = adapter.ListCustomerPhoneNumbers(1, "CUS-404", 100);

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
    }
}
