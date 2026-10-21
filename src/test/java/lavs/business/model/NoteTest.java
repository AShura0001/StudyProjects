package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class NoteTest {

    @Test
    void newNote_withAllValues_keepsThem() {
        Note note = new Note("Alex", "Jane Doe", "C-1", "2026-10-01", 2L, "Called about the loan", "1", "General");

        assertEquals("Alex", note.get_author());
        assertEquals("Jane Doe", note.get_customerName());
        assertEquals("C-1", note.get_customerNumber());
        assertEquals("2026-10-01", note.get_dateAdded());
        assertEquals(2L, note.get_id());
        assertEquals("Called about the loan", note.get_lines());
        assertEquals("1", note.get_numberOfLines());
        assertEquals("General", note.get_type());
    }

    @Test
    void setters_changeEveryProperty() {
        Note note = new Note();

        note.set_author("Sam");
        note.set_customerName("Sam Doe");
        note.set_customerNumber("C-2");
        note.set_dateAdded("2026-10-02");
        note.set_id(12L);
        note.set_lines("First line\nSecond line");
        note.set_number("N-1");
        note.set_numberOfLines("2");
        note.set_type("Reminder");

        assertEquals("Sam", note.get_author());
        assertEquals("Sam Doe", note.get_customerName());
        assertEquals("C-2", note.get_customerNumber());
        assertEquals("2026-10-02", note.get_dateAdded());
        assertEquals(12L, note.get_id());
        assertEquals("First line\nSecond line", note.get_lines());
        assertEquals("N-1", note.get_number());
        assertEquals("2", note.get_numberOfLines());
        assertEquals("Reminder", note.get_type());
    }

    @Test
    void note_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Note());
    }
}
