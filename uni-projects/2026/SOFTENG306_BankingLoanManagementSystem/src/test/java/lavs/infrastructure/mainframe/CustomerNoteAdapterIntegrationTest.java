package lavs.infrastructure.mainframe;

import lavs.business.model.Note;
import lavs.business.repository.CustomerSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerNoteAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private CustomerNoteAdapter adapter;

    @BeforeEach
    void createAdapter() {
        adapter = new CustomerNoteAdapter(connection);
    }

    @Test
    void listCustomerNotesReturnsAddedNotes() throws Exception {
        List<Long> createdIds = new ArrayList<>();
        try {
            Note first = addNote("List test note 1", "Line 1");
            Note second = addNote("List test note 2", "Line 1\nLine 2");
            createdIds.add(first.get_id());
            createdIds.add(second.get_id());

            List<Note> listed = fetchAllPages(
                    (page, size) -> adapter.ListCustomerNotes(page, CUSTOMER_NUMBER, size));

            assertListed(listed, first);
            assertListed(listed, second);
            for (Note note : listed) {
                assertEquals(CUSTOMER_NUMBER, note.get_customerNumber());
                assertNotNull(note.get_customerName());
            }
        } finally {
            for (long id : createdIds) {
                adapter.deleteNoteByCustomerNumberAndId(id, CUSTOMER_NUMBER);
            }
        }
    }

    @Test
    void listCustomerNotesWithUnknownCustomerReturnsEmptyResult() throws Exception {
        CustomerSearchResult<Note> result =
                adapter.ListCustomerNotes(1, "CUSTOMER-DOES-NOT-EXIST", 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void listCustomerNotesRespectsPageSize() throws Exception {
        CustomerSearchResult<Note> firstPage = adapter.ListCustomerNotes(1, CUSTOMER_NUMBER, 1);
        CustomerSearchResult<Note> secondPage = adapter.ListCustomerNotes(2, CUSTOMER_NUMBER, 1);

        assertEquals(1, firstPage.getPage());
        assertEquals(2, secondPage.getPage());
        assertTrue(firstPage.getList().size() <= 1);
        assertTrue(secondPage.getList().size() <= 1);

        if (!firstPage.getList().isEmpty() && !secondPage.getList().isEmpty()) {
            assertNotEquals(
                    firstPage.getList().get(0).get_id(),
                    secondPage.getList().get(0).get_id(),
                    "Different pages should not return the same note");
        }
    }

    private Note addNote(String type, String lines) throws Exception {
        Note note = new Note();
        note.set_customerNumber(CUSTOMER_NUMBER);
        note.set_type(type);
        note.set_lines(lines);

        Optional<Note> added = adapter.addCustomerNote(note);
        assertTrue(added.isPresent(), "Could not add note " + type);
        return added.get();
    }

    private static void assertListed(List<Note> listed, Note expected) {
        Note actual = listed.stream()
                .filter(n -> n.get_id() == expected.get_id())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Note " + expected.get_id() + " was not in the list"));

        assertEquals(expected.get_type(), actual.get_type());
        assertEquals(expected.get_author(), actual.get_author());
        assertEquals(expected.get_dateAdded(), actual.get_dateAdded());
    }
}
