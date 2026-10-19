package lavs.infrastructure.mainframe;

import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.messages.customer.AddPhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerPhoneNumberAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private CustomerPhoneNumberAdapter adapter;

    @BeforeEach
    void createAdapter() {
        adapter = new CustomerPhoneNumberAdapter(connection);
    }

    @Test
    void listCustomerPhoneNumbersReturnsAddedPhoneNumbers() throws Exception {
        List<Long> createdIds = new ArrayList<>();
        try {
            PhoneNumber first = addPhoneNumber("9-111-1111", true, AddPhoneNumber.TYPE_HOME);
            PhoneNumber second = addPhoneNumber("21-222-2222", false, AddPhoneNumber.TYPE_MOBILE);
            createdIds.add(first.get_id());
            createdIds.add(second.get_id());

            List<PhoneNumber> listed = fetchAllPages(
                    (page, size) -> adapter.ListCustomerPhoneNumbers(page, CUSTOMER_NUMBER, size));

            assertListed(listed, first.get_id(), "9-111-1111", true, AddPhoneNumber.TYPE_HOME);
            assertListed(listed, second.get_id(), "21-222-2222", false, AddPhoneNumber.TYPE_MOBILE);
            for (PhoneNumber phoneNumber : listed) {
                assertEquals(CUSTOMER_NUMBER, phoneNumber.get_customerNumber());
                assertNotNull(phoneNumber.get_customerName());
            }
        } finally {
            for (long id : createdIds) {
                adapter.deleteCustomerNumberByNumberAndId(id, CUSTOMER_NUMBER);
            }
        }
    }

    @Test
    void listCustomerPhoneNumbersWithUnknownCustomerReturnsEmptyResult() throws Exception {
        CustomerSearchResult<PhoneNumber> result =
                adapter.ListCustomerPhoneNumbers(1, "CUSTOMER-DOES-NOT-EXIST", 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void listCustomerPhoneNumbersRespectsPageSize() throws Exception {
        CustomerSearchResult<PhoneNumber> firstPage = adapter.ListCustomerPhoneNumbers(1, CUSTOMER_NUMBER, 1);
        CustomerSearchResult<PhoneNumber> secondPage = adapter.ListCustomerPhoneNumbers(2, CUSTOMER_NUMBER, 1);

        assertEquals(1, firstPage.getPage());
        assertEquals(2, secondPage.getPage());
        assertTrue(firstPage.getList().size() <= 1);
        assertTrue(secondPage.getList().size() <= 1);

        if (!firstPage.getList().isEmpty() && !secondPage.getList().isEmpty()) {
            assertNotEquals(
                    firstPage.getList().get(0).get_id(),
                    secondPage.getList().get(0).get_id(),
                    "Different pages should not return the same phone number");
        }
    }

    private PhoneNumber addPhoneNumber(String number, boolean canTxt, String type) throws Exception {
        PhoneNumber phoneNumber = new PhoneNumber(
                canTxt, "+64", null, CUSTOMER_NUMBER, 0, false, null, type);
        phoneNumber.set_number(number);

        Optional<PhoneNumber> added = adapter.addCustomerPhoneNumber(phoneNumber);
        assertTrue(added.isPresent(), "Could not add phone number " + number);
        return added.get();
    }

    private static void assertListed(
            List<PhoneNumber> listed, long id, String number, boolean canTxt, String type) {
        PhoneNumber actual = listed.stream()
                .filter(p -> p.get_id() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Phone number " + id + " was not in the list"));

        assertEquals(number, actual.get_number());
        assertEquals("+64", actual.get_countryCode());
        assertEquals(canTxt, actual.is_canTxt());
        assertFalse(actual.is_isPrimary());
        assertEquals(type, actual.get_type());
    }
}
