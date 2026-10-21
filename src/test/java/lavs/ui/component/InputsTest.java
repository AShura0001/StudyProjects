package lavs.ui.component;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputsTest {

    /** Controls need the JavaFX toolkit running; the controls here are never shown. */
    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // started by another test class
        } catch (UnsupportedOperationException noDisplay) {
            Assumptions.abort("JavaFX cannot start here: " + noDisplay.getMessage());
        }
    }

    @Test
    void readsTheUsualWaysOfWritingADate() {
        LocalDate expected = LocalDate.of(2025, 1, 9);
        assertEquals(expected, Inputs.parseDate("09/01/2025"));
        assertEquals(expected, Inputs.parseDate("9/1/2025"));
        assertEquals(expected, Inputs.parseDate("9-1-2025"));
        assertEquals(expected, Inputs.parseDate("9.1.2025"));
        assertEquals(expected, Inputs.parseDate("2025-01-09"));
    }

    @Test
    void refusesDatesThatDoNotExistOrAreUnfinished() {
        assertNull(Inputs.parseDate("31/02/2025"));
        assertNull(Inputs.parseDate("1/2/202"));
        assertNull(Inputs.parseDate("tomorrow"));
        assertNull(Inputs.parseDate(""));
    }

    @Test
    void datePickerReadsAndWritesIsoText() {
        DatePicker picker = new DatePicker();
        Inputs.date(picker);

        Inputs.setValue(picker, "2025-01-09");
        assertEquals("09/01/2025", picker.getEditor().getText());
        assertEquals("2025-01-09", Inputs.valueOf(picker));

        picker.getEditor().setText("3/4/2026");
        assertEquals("2026-04-03", Inputs.valueOf(picker), "typed text counts without pressing Enter");
    }

    @Test
    void badDatesNamesOnlyPickersWithUnreadableText() {
        DatePicker good = new DatePicker();
        DatePicker bad = new DatePicker();
        DatePicker empty = new DatePicker();
        Inputs.date(good);
        Inputs.date(bad);
        Inputs.date(empty);
        good.getEditor().setText("01/01/2025");
        bad.getEditor().setText("32/01/2025");

        Map<String, javafx.scene.control.Control> fields = new LinkedHashMap<>();
        fields.put("good", good);
        fields.put("bad", bad);
        fields.put("empty", empty);
        fields.put("text", new TextField("anything"));

        assertEquals(java.util.Set.of("bad"), Inputs.badDates(fields).keySet());
    }

    @Test
    void numberBoxRefusesLettersAndExtraDecimals() {
        TextField field = new TextField();
        Inputs.number(field, 2);

        field.setText("1250.5");
        assertEquals("1250.5", field.getText());
        field.setText("12a");
        assertEquals("1250.5", field.getText(), "letters are refused");
        field.setText("1.234");
        assertEquals("1250.5", field.getText(), "a third decimal place is refused");
        field.setText("$25,000.00");
        assertEquals("$25,000.00", field.getText(), "money as the screens show it is accepted");
    }

    @Test
    void phoneBoxOnlyTakesPhoneCharacters() {
        TextField field = new TextField();
        Inputs.phoneNumber(field);

        field.setText("(021) 555-0100");
        assertEquals("(021) 555-0100", field.getText());
        field.setText("021 abc");
        assertEquals("(021) 555-0100", field.getText());
    }

    @Test
    void dropDownKeepsAValueItDoesNotList() {
        ComboBox<String> box = new ComboBox<>();
        Inputs.choices(box, Choices.PAYMENT_FREQUENCIES, Choices::label);

        Inputs.setValue(box, "Monthly");
        assertEquals("Monthly", Inputs.valueOf(box));

        Inputs.setValue(box, "Quarterly");
        assertEquals("Quarterly", Inputs.valueOf(box));
        assertTrue(box.getItems().contains("Quarterly"));

        Inputs.setValue(box, "");
        assertEquals("", Inputs.valueOf(box));
    }

    @Test
    void labelsReadNaturallyButValuesStayAsTheMainframeSpellsThem() {
        assertEquals("Interest only", Choices.label("InterestOnly"));
        assertEquals("On renewal", Choices.label("Onrenewal"));
        assertEquals("Co-borrower", Choices.label("Coborrower"));
        assertEquals("Monthly", Choices.label("Monthly"));
        assertEquals("+64  New Zealand", Choices.codeLabel("+64"));
        assertTrue(Choices.COUNTRIES.contains("New Zealand"));
    }

    @Test
    void noticeShowsEachMessageOnce() {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("a", "Enter dates as DD/MM/YYYY.");
        errors.put("b", "Enter dates as DD/MM/YYYY.");
        errors.put("c", "Amount is required.");
        assertEquals("Enter dates as DD/MM/YYYY.  Amount is required.", Inputs.notice(errors));
    }
}
