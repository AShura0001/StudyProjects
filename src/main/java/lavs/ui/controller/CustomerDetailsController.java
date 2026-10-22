package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import lavs.business.model.Address;
import lavs.business.model.ContactChanges;
import lavs.business.model.Customer;
import lavs.business.model.Email;
import lavs.business.model.LoanListing;
import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;
import lavs.business.service.CustomerService;
import lavs.business.service.LoanAccountService;
import lavs.ui.Navigator;
import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Format;
import lavs.ui.component.Inputs;
import lavs.ui.component.NotesPaper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Customer Details with a view mode and an edit mode. Reads through CustomerService
 * and LoanAccountService and saves through CustomerService.saveContactChanges.
 * Edit changes are held in the form until Save; Cancel (or leaving) discards them
 * after confirmation. Every address and note is listed; they are added and changed on
 * their own forms, and removed here in edit mode (applied on Save).
 */
public class CustomerDetailsController {

    /** What the screen shows, taken from the business models. */
    private record Details(String customerNumber, String firstName, String lastName,
                           String phoneCode, String phoneNumber, String email, List<NoteRow> notes,
                           List<AddressRow> addresses, List<EmployerRow> employers, List<LoanListing> loans,
                           List<UnsentLoan> unsentLoans) {
    }

    /** A loan saved here that the mainframe has not confirmed yet. */
    private record UnsentLoan(String title, String state) {
    }

    /** One note: its title, the day it was added and who added it, and its text. */
    private record NoteRow(long id, String title, String added, String text) {
    }

    /** One employer: its name and whether it is the customer's primary or secondary employer. */
    private record EmployerRow(long id, String name, String type) {
    }

    /** One address: its street line, the rest of it, its type, and its flags. */
    private record AddressRow(long id, String street, String place, String type, boolean primary, boolean mailing) {
    }

    private static final String DEFAULT_PHONE_CODE = "+64";
    private static final java.util.regex.Pattern PHONE_DIGITS = java.util.regex.Pattern.compile("[0-9 ()-]{0,20}");

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label customerIdLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> phoneCodeBox;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private VBox addressesBox;
    @FXML private TextArea notesArea;
    @FXML private ScrollPane notesList;
    @FXML private VBox notesBox;
    @FXML private VBox employersBox;
    @FXML private VBox loansBox;
    @FXML private Label noticeLabel;
    @FXML private Button viewModeButton;
    @FXML private Button editModeButton;
    @FXML private Button photoEditButton;
    @FXML private Node editActions;
    @FXML private StackPane photoCard;
    @FXML private StackPane avatarBox;

    private final CustomerService customers = LavsApp.context().customers();
    private final LoanAccountService loanAccounts = LavsApp.context().loanAccounts();
    private Runnable onBack = () -> Navigator.show("customersearch");

    private Details details;
    private List<EmployerRow> workingEmployers = new ArrayList<>();
    private List<AddressRow> workingAddresses = new ArrayList<>();
    private List<NoteRow> workingNotes = new ArrayList<>();
    private boolean editing;
    private boolean busy;
    /** Lets the phone field show a stored number exactly, even one the edit filter would refuse. */
    private boolean settingPhone;

    @FXML
    private void initialize() {
        NotesPaper.attach(notesArea);
        Inputs.shortChoices(phoneCodeBox, Choices.COUNTRY_CODES, Choices::codeLabel);
        // In edit mode the code comes from the drop-down, so the number box only takes digits and spacing.
        phoneField.setTextFormatter(new TextFormatter<String>(change -> settingPhone || !editing
                || PHONE_DIGITS.matcher(change.getControlNewText()).matches() ? change : null));
        Inputs.onEdit(phoneCodeBox, () -> phoneField.getStyleClass().remove("field-error"));
        setShown(noticeLabel, false);
        setShown(progress, false);
        applyMode(false);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
    }

    /**
     * Called by the previous screen before this one is displayed.
     *
     * @param back what Back should do, so the user returns to where they came from
     */
    public void open(String customerNumber, Runnable back) {
        onBack = back;
        customerIdLabel.setText(customerNumber);
        load(customerNumber, "");
    }

    private void load(String customerNumber, String noticeWhenDone) {
        setBusy(true);
        Task<Details> task = new Task<>() {
            @Override
            protected Details call() throws Exception {
                return read(customerNumber);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            details = task.getValue();
            if (details == null) {
                showNotice("Could not find customer " + customerNumber + ".");
            } else {
                revert();
                showNotice(noticeWhenDone);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(Format.problem(task.getException(), "Could not reach the mainframe. Try again."));
        });
        startWorker(task, "customer-details");
    }

    /** Runs off the JavaFX thread: everything the screen shows, from the business services. */
    private Details read(String customerNumber) throws Exception {
        Optional<Customer> found = customers.getCustomer(customerNumber);
        if (found.isEmpty()) {
            return null;
        }
        Customer customer = found.get();
        PhoneNumber phone = customers.primaryPhone(customerNumber).orElse(null);
        Email email = customers.primaryEmail(customerNumber).orElse(null);
        List<AddressRow> addresses = customers.listAddressesInFull(customerNumber).stream()
                .map(CustomerDetailsController::addressRow)
                .toList();
        List<EmployerRow> employers = customers.listEmployers(customerNumber).stream()
                .map(e -> new EmployerRow(e.get_id(),
                        e.get_name() == null || e.get_name().isBlank() ? "Employer" : e.get_name(),
                        Format.text(e.get_type())))
                .toList();
        List<NoteRow> notes = customers.listNotes(customerNumber).stream()
                .map(n -> new NoteRow(n.get_id(),
                        Format.text(n.get_type()).isEmpty() ? CustomerService.DEFAULT_NOTE_TYPE : Format.text(n.get_type()),
                        joined(" • ", dateOnly(n.get_dateAdded()), n.get_author()),
                        n.get_lines() == null ? "" : n.get_lines().strip()))
                .toList();
        return new Details(customerNumber, Format.text(customer.get_firstName()), Format.text(customer.get_familyName()),
                phone == null ? "" : Format.text(phone.get_countryCode()),
                phone == null ? "" : Format.text(phone.get_number()),
                email == null ? "" : Format.text(email.getAddress()),
                notes, addresses, employers, loanAccounts.loansOf(customerNumber), unsentLoans(customerNumber));
    }

    private List<UnsentLoan> unsentLoans(String customerNumber) {
        List<UnsentLoan> rows = new ArrayList<>();
        for (lavs.business.model.Draft draft : LavsApp.context().loans().unsentLoansOf(customerNumber)) {
            String title = "New loan";
            try {
                title = "New loan of " + Format.money(
                        lavs.business.model.LoanPayloads.readLoan(draft.getPayload()).details().principal());
            } catch (IllegalArgumentException ignored) {
                // shown as plain "New loan"
            }
            String state = switch (draft.getStatus()) {
                case UNCERTAIN -> "Needs checking";
                case REJECTED -> "Refused by the mainframe";
                default -> "Not sent yet";
            };
            rows.add(new UnsentLoan(title, state));
        }
        return rows;
    }

    private static AddressRow addressRow(Address address) {
        String street = joined(", ", address.getLine1(), address.getLine2());
        String place = joined(", ", address.getSuburb(), joined(" ", address.getCity(), address.getPostCode()),
                address.getCountry());
        String type = Format.text(address.getType());
        if ("Other".equalsIgnoreCase(type) && !Format.text(address.getOtherType()).isEmpty()) {
            type = Format.text(address.getOtherType());
        }
        return new AddressRow(address.getId(), street.isEmpty() ? "(no street line)" : street, place, type,
                Boolean.TRUE.equals(address.getIsPrimary()), Boolean.TRUE.equals(address.getIsMailing()));
    }

    private static String dateOnly(String text) {
        String t = Format.text(text);
        return t.length() > 10 ? t.substring(0, 10) : t;
    }

    /** The notepad shows every note under its own title line. */
    private static String notepadText(List<NoteRow> notes) {
        return notes.stream()
                .map(n -> n.title() + (n.added().isEmpty() ? "" : "  (" + n.added() + ")") + "\n" + n.text())
                .collect(Collectors.joining("\n\n"));
    }

    private static String joined(String separator, String... parts) {
        return Stream.of(parts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.joining(separator));
    }

    /** Puts the form, notes and employer list back to the last loaded values. */
    private void revert() {
        firstNameField.setText(details.firstName());
        lastNameField.setText(details.lastName());
        showPhone();
        emailField.setText(details.email());
        notesArea.setText(notepadText(details.notes()));
        workingEmployers = new ArrayList<>(details.employers());
        workingAddresses = new ArrayList<>(details.addresses());
        workingNotes = new ArrayList<>(details.notes());
        clearFieldErrors();
        renderNotes();
        renderAddresses();
        renderEmployers();
        renderLoans();
    }

    private boolean isDirty() {
        return details != null && !details.equals(readForm());
    }

    private Details readForm() {
        String code = editing ? phoneCodeBox.getValue() : details.phoneCode();
        String number = editing ? phoneField.getText().trim() : details.phoneNumber();
        if (editing && details.phoneNumber().isEmpty() && number.isEmpty()) {
            code = details.phoneCode();
        }
        return new Details(details.customerNumber(),
                firstNameField.getText().trim(), lastNameField.getText().trim(),
                code == null ? "" : code, number, emailField.getText().trim(), List.copyOf(workingNotes),
                List.copyOf(workingAddresses), List.copyOf(workingEmployers), details.loans(), details.unsentLoans());
    }

    /** Edit mode lists the notes one by one, each with remove and Edit. */
    private void renderNotes() {
        notesBox.getChildren().clear();
        if (workingNotes.isEmpty()) {
            Label none = new Label("No notes yet.");
            none.getStyleClass().add("note-row-text");
            none.setPadding(new javafx.geometry.Insets(4, 6, 4, 6));
            notesBox.getChildren().add(none);
        }
        for (NoteRow note : workingNotes) {
            Label title = new Label(note.title());
            title.getStyleClass().add("note-row-title");
            String preview = note.text().replace('\n', ' ');
            Label text = new Label(preview.length() > 60 ? preview.substring(0, 57) + "…" : preview);
            text.getStyleClass().add("note-row-text");
            VBox words = new VBox(1, title, text);
            HBox.setHgrow(words, Priority.ALWAYS);
            words.setMinWidth(0);

            Button remove = new Button();
            remove.getStyleClass().add("btn-remove");
            SVGPath cross = new SVGPath();
            cross.setContent("M5 5 L19 19 M19 5 L5 19");
            cross.getStyleClass().add("remove-icon");
            remove.setGraphic(cross);
            remove.setOnAction(e -> {
                workingNotes.remove(note);
                renderNotes();
            });
            Button edit = new Button("Edit");
            edit.getStyleClass().add("btn-edit-chip");
            edit.setOnAction(e -> openNoteEditor(note.id()));

            // The buttons keep their size; the preview gives way and ends in "…".
            remove.setMinWidth(Region.USE_PREF_SIZE);
            edit.setMinWidth(Region.USE_PREF_SIZE);
            text.setMinWidth(0);
            title.setMinWidth(0);
            HBox row = new HBox(4, words, remove, edit);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("note-row");
            notesBox.getChildren().add(row);
        }
    }

    @FXML
    private void onAddNote() {
        if (details != null) {
            openNoteEditor(null);
        }
    }

    /** @param noteId the note to change, or null to add one */
    private void openNoteEditor(Long noteId) {
        if (!confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Runnable reopen = () -> Navigator.<CustomerDetailsController>show("customerdetails",
                c -> c.open(customerNumber, backFromCustomer));
        Navigator.<EditNoteController>show("editnote",
                n -> n.open(customerNumber, noteId, reopen, reopen));
    }

    private void renderAddresses() {
        addressesBox.getChildren().clear();
        if (workingAddresses.isEmpty()) {
            Label none = new Label("No addresses yet.");
            none.getStyleClass().add("list-item-sub");
            addressesBox.getChildren().add(none);
        }
        for (AddressRow address : workingAddresses) {
            addressesBox.getChildren().add(addressItem(address));
        }
    }

    /** Street line, then place and type with Primary / Mailing tags; in edit mode, remove and Edit. */
    private Node addressItem(AddressRow address) {
        Label street = new Label(address.street());
        street.getStyleClass().add("list-item-title");
        HBox tags = new HBox(4);
        tags.setAlignment(Pos.CENTER_LEFT);
        Label place = new Label(joined("  •  ", address.place(), address.type()));
        place.getStyleClass().add("list-item-sub");
        tags.getChildren().add(place);
        if (address.primary()) {
            tags.getChildren().add(tag("PRIMARY", "address-tag"));
        }
        if (address.mailing()) {
            tags.getChildren().add(tag("MAILING", "address-tag-mailing"));
        }
        VBox text = new VBox(1, street, tags);
        text.getStyleClass().add("list-item");
        if (!editing) {
            return text;
        }

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setOnAction(e -> {
            workingAddresses.remove(address);
            renderAddresses();
        });

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setOnAction(e -> openAddressEditor(address.id()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, text, spacer, remove, edit);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private static Label tag(String text, String style) {
        Label tag = new Label(text);
        tag.getStyleClass().addAll("address-tag", style);
        return tag;
    }

    /** Opens every phone number and email of the customer, where they are added, changed and removed. */
    @FXML
    private void onManageContacts() {
        if (details == null || busy || !confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Navigator.<ContactsController>show("contacts", c -> c.open(customerNumber,
                () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        d -> d.open(customerNumber, backFromCustomer))));
    }

    @FXML
    private void onAddAddress() {
        if (details != null) {
            openAddressEditor(null);
        }
    }

    /** @param addressId the address to change, or null to add one */
    private void openAddressEditor(Long addressId) {
        if (!confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Runnable reopen = () -> Navigator.<CustomerDetailsController>show("customerdetails",
                c -> c.open(customerNumber, backFromCustomer));
        Navigator.<EditAddressController>show("editaddress",
                a -> a.open(customerNumber, addressId, reopen, reopen));
    }

    private void renderEmployers() {
        employersBox.getChildren().clear();
        for (EmployerRow employer : workingEmployers) {
            employersBox.getChildren().add(employerItem(employer));
        }
    }

    private void renderLoans() {
        loansBox.getChildren().clear();
        for (LoanListing loan : details.loans()) {
            VBox item = listItem(Format.loanName(loan.loanNumber()),
                    Format.date(loan.startDate()) + "  •  " + loan.loanNumber());
            Hyperlink link = new Hyperlink("Click here for further details");
            link.getStyleClass().add("list-item-link");
            link.setOnAction(e -> openLoan(loan.loanNumber()));
            item.getChildren().add(link);
            loansBox.getChildren().add(item);
        }
        for (UnsentLoan loan : details.unsentLoans()) {
            VBox item = listItem(loan.title(), loan.state());
            Hyperlink link = new Hyperlink("See saved changes");
            link.getStyleClass().add("list-item-link");
            link.setOnAction(e -> DraftsController.openWindow((javafx.stage.Stage) root.getScene().getWindow()));
            item.getChildren().add(link);
            loansBox.getChildren().add(item);
        }
    }

    private void openLoan(String loanNumber) {
        if (!confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Navigator.<LoanDetailsController>show("loandetails", l -> l.open(loanNumber,
                () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(customerNumber, backFromCustomer))));
    }

    private void openEmployerEditor(EmployerRow employer) {
        if (!confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Runnable reopen = () -> Navigator.<CustomerDetailsController>show("customerdetails",
                c -> c.open(customerNumber, backFromCustomer));
        Navigator.<EditEmployerController>show("editemployer",
                e -> e.open(customerNumber, employer == null ? null : String.valueOf(employer.id()), reopen, reopen));
    }

    @FXML
    private void onAddEmployer() {
        if (details != null) {
            openEmployerEditor(null);
        }
    }

    private VBox listItem(String title, String subtitle) {
        VBox item = new VBox(1);
        item.getStyleClass().add("list-item");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("list-item-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("list-item-sub");
        item.getChildren().addAll(titleLabel, subtitleLabel);
        return item;
    }

    /** In edit mode each employer row gains a remove and an Edit button. */
    private Node employerItem(EmployerRow employer) {
        VBox text = listItem(employer.name(), employer.type().isEmpty() ? "Employer" : employer.type() + " employer");
        if (!editing) {
            return text;
        }

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setOnAction(e -> {
            workingEmployers.remove(employer);
            renderEmployers();
        });

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setOnAction(e -> openEmployerEditor(employer));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, text, spacer, remove, edit);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    /** Switches every editable part of the screen between view and edit mode. */
    private void applyMode(boolean edit) {
        editing = edit;
        for (TextField field : List.of(firstNameField, lastNameField, phoneField, emailField)) {
            field.setEditable(edit);
            field.setFocusTraversable(edit);
        }
        // View mode reads the notes on the notepad; edit mode lists them so each can be changed or removed.
        notesArea.setEditable(false);
        notesArea.setFocusTraversable(false);
        setShown(notesArea.getParent(), !edit);
        setShown(notesList, edit);
        setShown(phoneCodeBox, edit);
        phoneCodeBox.setFocusTraversable(edit);
        showPhone();

        toggleStyle(content, "editing", edit);
        toggleStyle(viewModeButton, "mode-selected", !edit);
        toggleStyle(editModeButton, "mode-selected", edit);
        // Photos have no business service behind them yet.
        setShown(photoEditButton, false);
        setShown(editActions, edit);
        clearFieldErrors();
        if (details != null) {
            renderNotes();
            renderAddresses();
            renderEmployers();
        }
        if (edit) {
            Platform.runLater(firstNameField::requestFocus);
        }
    }

    /**
     * View mode shows the whole number, e.g. "+64 21 555 0100". Edit mode splits it:
     * the country code goes in the drop-down and the rest in the number box.
     */
    private void showPhone() {
        settingPhone = true;
        try {
            if (details == null) {
                phoneField.setText("");
                return;
            }
            if (!editing) {
                String code = details.phoneCode();
                phoneField.setText(code.isEmpty() || details.phoneNumber().isEmpty()
                        ? details.phoneNumber() : code + " " + details.phoneNumber());
                return;
            }
            String code = details.phoneCode().isEmpty()
                    ? (details.phoneNumber().isEmpty() ? DEFAULT_PHONE_CODE : null)
                    : details.phoneCode();
            Inputs.setValue(phoneCodeBox, code);
            phoneField.setText(details.phoneNumber());
        } finally {
            settingPhone = false;
        }
    }

    private static void toggleStyle(Node node, String styleClass, boolean on) {
        node.getStyleClass().remove(styleClass);
        if (on) {
            node.getStyleClass().add(styleClass);
        }
    }

    @FXML
    private void onEditMode() {
        if (!busy && details != null && !editing) {
            showNotice("");
            applyMode(true);
        }
    }

    @FXML
    private void onViewMode() {
        if (!busy && editing && confirmDiscard()) {
            revert();
            applyMode(false);
        }
    }

    @FXML
    private void onCancel() {
        onViewMode();
    }

    @FXML
    private void onSave() {
        if (busy || !editing || details == null) {
            return;
        }
        Details edited = readForm();
        List<Long> removed = details.employers().stream()
                .filter(e -> !edited.employers().contains(e))
                .map(EmployerRow::id)
                .toList();
        List<Long> removedAddresses = details.addresses().stream()
                .filter(a -> !edited.addresses().contains(a))
                .map(AddressRow::id)
                .toList();
        List<Long> removedNotes = details.notes().stream()
                .filter(n -> !edited.notes().contains(n))
                .map(NoteRow::id)
                .toList();
        ContactChanges changes = new ContactChanges(edited.customerNumber(), edited.firstName(), edited.lastName(),
                edited.phoneCode(), edited.phoneNumber(), edited.email(), removed, removedAddresses, removedNotes);
        clearFieldErrors();
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                customers.saveContactChanges(changes);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            applyMode(false);
            // Read back what the mainframe now holds.
            load(edited.customerNumber(), "Changes saved.");
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            if (Format.isRuleError(error)) {
                showFieldErrors(Format.fieldErrors(error, Map.of("firstName", "firstName", "familyName", "lastName",
                        "phone", "phone", "email", "email", "addresses", "addresses")));
            } else {
                showNotice(Format.problem(error, "Could not reach the mainframe. Your changes are still on screen."));
            }
        });

        startWorker(task, "customer-save");
    }

    private void showFieldErrors(Map<String, String> errors) {
        Map<String, TextField> fields = Map.of(
                "firstName", firstNameField, "lastName", lastNameField, "phone", phoneField,
                "email", emailField);
        for (String key : errors.keySet()) {
            TextField field = fields.get(key);
            if (field != null) {
                toggleStyle(field, "field-error", true);
            }
        }
        showNotice(Inputs.notice(errors));
        errors.keySet().stream().map(fields::get).filter(Objects::nonNull).findFirst()
                .ifPresent(TextField::requestFocus);
    }

    private void clearFieldErrors() {
        for (TextField field : List.of(firstNameField, lastNameField, phoneField, emailField)) {
            toggleStyle(field, "field-error", false);
        }
    }

    /** The photo button stays hidden: no business service stores photos yet. */
    @FXML
    private void onPhotoEdit() {
        showNotice("Photos cannot be stored on the mainframe yet.");
    }

    @FXML
    private void onAddLoan() {
        if (details == null || !confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Navigator.<AddLoanController>show("addloan", a -> a.open(customerNumber,
                () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(customerNumber, backFromCustomer))));
    }

    @FXML
    private void onBack() {
        if (confirmDiscard()) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
        if (confirmDiscard()) {
            LavsApp.context().logout();
            Navigator.show("loggedout");
        }
    }

    /** Esc cancels an edit in progress; otherwise it goes back. */
    private void onEscape() {
        if (editing) {
            onCancel();
        } else {
            onBack();
        }
    }

    /** True when there is nothing to lose or the user agrees to discard their edits. */
    private boolean confirmDiscard() {
        if (!editing || !isDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Discard them?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onEscape);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), this::onEditMode);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSave);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        setShown(progress, busy);
        content.setOpacity(busy ? 0.35 : 1.0);
        content.setDisable(busy);
    }

    private void startWorker(Task<?> task, String name) {
        Thread worker = new Thread(task, name);
        worker.setDaemon(true);
        worker.start();
    }

    private void showNotice(String text) {
        noticeLabel.setText(text);
        setShown(noticeLabel, !text.isEmpty());
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard shortcuts");
        alert.setHeaderText("Customer details");
        alert.setContentText("""
                Ctrl+E     Edit mode
                Ctrl+S     Save (in edit mode)
                Esc        Cancel editing, or go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
