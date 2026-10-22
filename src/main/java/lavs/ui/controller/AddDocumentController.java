package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import lavs.business.service.DocumentService;
import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Records a document against a customer or a loan. Choosing a file fills in its name,
 * kind of file and size; only those details are sent. The rules (a file name and a kind
 * of document) are checked by {@link DocumentService}; problems come back against the fields.
 */
public class AddDocumentController extends FormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField fileNameField;
    @FXML private Label fileInfoLabel;

    private final DocumentService documents = LavsApp.context().documents();
    private DocumentsController.Owner owner = DocumentsController.Owner.CUSTOMER;
    private String ownerNumber;
    /** The name, kind and size of the chosen file; the kind is empty when the name was typed instead. */
    private String chosenName = "";
    private String fileType = "";
    private long size;

    @FXML
    private void initialize() {
        Inputs.choices(typeField, Choices.DOCUMENT_TYPES, t -> t);
        // A typed name has no file behind it, so forget the kind and size of an earlier choice.
        fileNameField.textProperty().addListener((obs, old, now) -> {
            if (!fileInfoLabel.getText().isEmpty() && !now.equals(chosenName)) {
                fileType = "";
                size = 0;
                fileInfoLabel.setText("");
            }
        });
        register(DocumentService.TYPE_FIELD, typeField);
        register(DocumentService.FILE_NAME_FIELD, fileNameField);
        setUp(typeField);
    }

    /**
     * Called by the documents screen before this one is displayed.
     *
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(DocumentsController.Owner owner, String ownerNumber, Runnable back, Runnable done) {
        this.owner = owner;
        this.ownerNumber = ownerNumber;
        this.onBack = back;
        this.onDone = done;
        titleLabel.setText(owner == DocumentsController.Owner.CUSTOMER
                ? "Record a customer document" : "Record a loan document");
        markClean();
    }

    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the document");
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        chosenName = file.getName();
        size = file.length();
        fileType = kindOf(file);
        fileNameField.setText(chosenName);
        fileInfoLabel.setText((fileType.isEmpty() ? "Unknown kind of file" : fileType)
                + "  •  " + DocumentsController.sizeOf(size));
    }

    /** The file's kind, such as application/pdf, as the computer knows it; empty if unknown. */
    private static String kindOf(File file) {
        try {
            String kind = Files.probeContentType(file.toPath());
            return kind == null ? "" : kind;
        } catch (IOException | SecurityException e) {
            return "";
        }
    }

    @Override
    protected void submitForm() {
        String type = typeField.getValue();
        String fileName = Inputs.valueOf(fileNameField);
        String kind = fileType;
        long bytes = size;
        DocumentsController.Owner whose = owner;
        String number = ownerNumber;
        save(() -> {
            if (whose == DocumentsController.Owner.CUSTOMER) {
                documents.addToCustomer(number, fileName, kind, bytes, type);
            } else {
                documents.addToLoan(number, fileName, kind, bytes, type);
            }
            return Map.of();
        });
    }

    @Override
    protected String what() {
        return "document";
    }

    @Override
    protected String screenName() {
        return "Record a document";
    }
}
