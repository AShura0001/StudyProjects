package lavs.ui.component;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Sets up the form controls so they only take sensible input: date pickers,
 * drop-downs and number boxes. Every control still reads and writes plain text
 * (a date as 2025-01-09, a choice as its value), so the services are unchanged.
 */
public final class Inputs {

    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final List<DateTimeFormatter> TYPED = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ISO_LOCAL_DATE);
    private static final String DATE_HELP = "Enter dates as DD/MM/YYYY, or pick them from the calendar.";

    private Inputs() {
    }

    // ---- Set-up ----

    /** Shows and accepts DD/MM/YYYY (YYYY-MM-DD also works) and offers a calendar. */
    public static void date(DatePicker picker) {
        picker.setPromptText("DD/MM/YYYY");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : SHOWN.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                return parseDate(text);
            }
        });
    }

    /** Greys out future days, for things like a date of birth. */
    public static void pastDatesOnly(DatePicker picker) {
        picker.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isAfter(LocalDate.now()));
            }
        });
    }

    /** Only digits, with up to {@code decimals} places; a leading $ and commas are allowed. */
    public static void number(TextInputControl field, int decimals) {
        String fraction = decimals > 0 ? "(\\.[0-9]{0," + decimals + "})?" : "";
        filter(field, Pattern.compile("\\$?[0-9,]{0,15}" + fraction));
    }

    /** Only the characters a phone number uses: digits, spaces, dashes and brackets. */
    public static void phoneNumber(TextInputControl field) {
        filter(field, Pattern.compile("[0-9 ()-]{0,20}"));
    }

    /** A drop-down of fixed values, shown through {@code label}; typing letters jumps to a match. */
    public static void choices(ComboBox<String> box, List<String> values, Function<String, String> label) {
        box.getItems().setAll(values);
        box.setVisibleRowCount(10);
        // Without this a list of long names widens the box and squeezes the fields beside it.
        if (box.getPrefWidth() == Region.USE_COMPUTED_SIZE) {
            box.setPrefWidth(150);
        }
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : label.apply(value);
            }

            @Override
            public String fromString(String text) {
                return box.getItems().stream().filter(v -> label.apply(v).equals(text)).findFirst().orElse(null);
            }
        });
        typeToJump(box, label);
    }

    /** Like {@link #choices}, but the closed box shows only the value, e.g. "+64". */
    public static void shortChoices(ComboBox<String> box, List<String> values, Function<String, String> label) {
        choices(box, values, label);
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
    }

    private static void filter(TextInputControl field, Pattern allowed) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                allowed.matcher(change.getControlNewText()).matches() ? change : null));
    }

    private static void typeToJump(ComboBox<String> box, Function<String, String> label) {
        StringBuilder typed = new StringBuilder();
        long[] lastKey = {0};
        EventHandler<KeyEvent> jump = e -> {
            String ch = e.getCharacter();
            if (ch.isEmpty() || Character.isISOControl(ch.charAt(0))) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastKey[0] > 1000) {
                typed.setLength(0);
            }
            lastKey[0] = now;
            typed.append(ch.toLowerCase(Locale.ROOT));
            String prefix = typed.toString();
            box.getItems().stream()
                    .filter(v -> label.apply(v).toLowerCase(Locale.ROOT).startsWith(prefix))
                    .findFirst()
                    .ifPresent(v -> {
                        box.setValue(v);
                        if (box.getSkin() instanceof ComboBoxListViewSkin<?> skin
                                && skin.getPopupContent() instanceof ListView<?> list) {
                            int index = box.getItems().indexOf(v);
                            // After the list has reacted to the new value, bring the match into view.
                            Platform.runLater(() -> list.scrollTo(Math.max(0, index - 2)));
                        }
                    });
            e.consume();
        };
        box.addEventHandler(KeyEvent.KEY_TYPED, jump);
        box.skinProperty().addListener((obs, old, skin) -> {
            if (skin instanceof ComboBoxListViewSkin<?> listSkin) {
                listSkin.getPopupContent().addEventHandler(KeyEvent.KEY_TYPED, jump);
            }
        });
    }

    // ---- Reading and writing any control as text ----

    /** The control's value as text: a date as 2025-01-09, a choice as its value, "" when empty. */
    public static String valueOf(Control control) {
        if (control instanceof TextInputControl text) {
            return text.getText().trim();
        }
        if (control instanceof DatePicker picker) {
            String typed = picker.getEditor().getText().trim();
            if (typed.isEmpty()) {
                return "";
            }
            LocalDate date = parseDate(typed);
            return date == null ? typed : date.toString();
        }
        if (control instanceof ComboBox<?> box) {
            return box.getValue() == null ? "" : box.getValue().toString();
        }
        if (control instanceof CheckBox tick) {
            return String.valueOf(tick.isSelected());
        }
        throw new IllegalArgumentException("Unsupported control: " + control);
    }

    /** Puts text into the control; a value a drop-down does not list yet is added so it still shows. */
    @SuppressWarnings("unchecked")
    public static void setValue(Control control, String value) {
        String text = value == null ? "" : value.trim();
        if (control instanceof TextInputControl field) {
            field.setText(text);
        } else if (control instanceof DatePicker picker) {
            LocalDate date = parseDate(text);
            picker.setValue(date);
            picker.getEditor().setText(date == null ? text : SHOWN.format(date));
        } else if (control instanceof ComboBox<?> any) {
            ComboBox<String> box = (ComboBox<String>) any;
            if (!text.isEmpty() && !box.getItems().contains(text)) {
                box.getItems().add(text);
            }
            box.setValue(text.isEmpty() ? null : text);
        } else if (control instanceof CheckBox tick) {
            tick.setSelected(Boolean.parseBoolean(text));
        } else {
            throw new IllegalArgumentException("Unsupported control: " + control);
        }
    }

    /** Runs {@code action} whenever the user changes the control. */
    public static void onEdit(Control control, Runnable action) {
        if (control instanceof TextInputControl field) {
            field.textProperty().addListener((obs, old, now) -> action.run());
        } else if (control instanceof DatePicker picker) {
            picker.valueProperty().addListener((obs, old, now) -> action.run());
            picker.getEditor().textProperty().addListener((obs, old, now) -> action.run());
        } else if (control instanceof ComboBox<?> box) {
            box.valueProperty().addListener((obs, old, now) -> action.run());
        } else if (control instanceof CheckBox tick) {
            tick.selectedProperty().addListener((obs, old, now) -> action.run());
        }
    }

    /** View mode: the control shows its value but cannot be changed or tabbed into. */
    public static void setLocked(Control control, boolean locked) {
        if (control instanceof TextInputControl field) {
            field.setEditable(!locked);
        } else {
            control.setDisable(locked);
        }
        control.setFocusTraversable(!locked);
    }

    // ---- Checks ----

    /** Field key to message for every date picker holding text that is not a date. */
    public static Map<String, String> badDates(Map<String, ? extends Control> fields) {
        Map<String, String> errors = new LinkedHashMap<>();
        fields.forEach((key, control) -> {
            if (control instanceof DatePicker picker) {
                String typed = picker.getEditor().getText().trim();
                if (!typed.isEmpty() && parseDate(typed) == null) {
                    errors.put(key, DATE_HELP);
                }
            }
        });
        return errors;
    }

    /** The messages to show together, each once. */
    public static String notice(Map<String, String> errors) {
        return String.join("  ", errors.values().stream().filter(m -> !m.isBlank()).distinct().toList());
    }

    static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DateTimeFormatter format : TYPED) {
            try {
                return LocalDate.parse(text.trim(), format);
            } catch (DateTimeParseException ignored) {
                // try the next way of writing it
            }
        }
        return null;
    }
}
