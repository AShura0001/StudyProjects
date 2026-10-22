package lavs.business.service;

import lavs.business.model.Document;
import lavs.business.model.LoanDocument;
import lavs.business.model.StoredDocument;
import lavs.business.repository.CustomerDocumentGateway;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.LoanDocumentGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.SessionExpiredException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The rules for the documents recorded against customers and loans.
 *
 * <p>The mainframe keeps each document's details (file name, file type, size and
 * what kind of document it is), not the file itself, so recording a document means
 * sending those details. A document needs a file name and a kind. Documents live on
 * the mainframe, so every call needs a connection, and staff get a clear message
 * when there is none. The mainframe cannot change a document once recorded; a wrong
 * one is removed and recorded again.
 *
 * <p>These calls contact the mainframe and can take a while, so a screen must call
 * them off its own thread.
 */
public class DocumentService {

    /** The field name used for a problem with the file name. */
    public static final String FILE_NAME_FIELD = "fileName";

    /** The field name used for a problem with the kind of document. */
    public static final String TYPE_FIELD = "type";

    /** Longest file name allowed, in characters. */
    public static final int MAX_FILE_NAME_LENGTH = 255;

    /** The file type recorded when the kind of file is not known. */
    public static final String UNKNOWN_FILE_TYPE = "application/octet-stream";

    /** Shown when the mainframe cannot be reached. */
    public static final String OFFLINE_MESSAGE =
            "Documents need a connection to the mainframe. Please try again when you are back online.";

    /** Shown when the mainframe did not record a document. */
    public static final String NOT_ADDED_MESSAGE = "The mainframe did not accept the document.";

    /** Shown when a document could not be removed. */
    public static final String NOT_REMOVED_MESSAGE = "The document could not be removed. It may no longer exist.";

    private static final long PAGE_SIZE = 100;
    private static final int MAX_PAGES = 1000;

    @FunctionalInterface
    private interface Call<T> {
        T run() throws IOException;
    }

    private final CustomerDocumentGateway customerDocuments;
    private final LoanDocumentGateway loanDocuments;

    /**
     * Creates the service.
     *
     * @param customerDocuments how a customer's documents are read and changed on the mainframe
     * @param loanDocuments how a loan's documents are read and changed on the mainframe
     * @throws NullPointerException if either gateway is null
     */
    public DocumentService(CustomerDocumentGateway customerDocuments, LoanDocumentGateway loanDocuments) {
        this.customerDocuments = Objects.requireNonNull(customerDocuments, "customerDocuments must not be null");
        this.loanDocuments = Objects.requireNonNull(loanDocuments, "loanDocuments must not be null");
    }

    /**
     * Gets every document recorded for a customer, each read in full (the mainframe's
     * list leaves out the file type and size).
     *
     * @return the documents, in the order the mainframe lists them; empty if there are none
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public List<StoredDocument> documentsOfCustomer(String customerNumber) {
        requireText(customerNumber, "customerNumber");
        return call(() -> {
            List<StoredDocument> all = new ArrayList<>();
            for (long page = 1; page <= MAX_PAGES; page++) {
                CustomerSearchResult<Document> result =
                        customerDocuments.ListCustomerDocuments(page, customerNumber, PAGE_SIZE);
                for (Document summary : result.getList()) {
                    Document full = customerDocuments.findDocumentByCustomerNumberAndId(summary.getId(), customerNumber)
                            .orElse(summary);
                    all.add(fromCustomer(customerNumber, full));
                }
                if (result.getList().isEmpty() || all.size() >= result.getTotal()) {
                    break;
                }
            }
            return all;
        });
    }

    /**
     * Records a document against a customer.
     *
     * @param fileType the kind of file; blank means {@value #UNKNOWN_FILE_TYPE}
     * @param size the size of the file in bytes
     * @param type what kind of document it is
     * @return the document as the mainframe recorded it
     * @throws ValidationException if the file name or kind is missing, or the file name is too long
     * @throws IllegalArgumentException if the size is negative
     * @throws RecordRejectedException if the mainframe did not accept the document
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public StoredDocument addToCustomer(String customerNumber, String fileName, String fileType, long size, String type) {
        requireText(customerNumber, "customerNumber");
        Details details = checked(fileName, fileType, size, type);
        Document document = new Document();
        document.setCustomerNumber(customerNumber);
        document.setFileName(details.fileName());
        document.setFileType(details.fileType());
        document.setSize(size);
        document.setType(details.type());
        Document saved = call(() -> customerDocuments.addCustomerDocument(document))
                .orElseThrow(() -> new RecordRejectedException(NOT_ADDED_MESSAGE, Map.of()));
        return fromCustomer(customerNumber, saved);
    }

    /**
     * Removes a document from a customer.
     *
     * @throws RecordRejectedException if the mainframe did not remove it
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public void removeFromCustomer(String customerNumber, long documentId) {
        requireText(customerNumber, "customerNumber");
        if (!call(() -> customerDocuments.deleteCustomerDocument(documentId, customerNumber))) {
            throw new RecordRejectedException(NOT_REMOVED_MESSAGE, Map.of());
        }
    }

    /**
     * Gets every document recorded for a loan, each read in full.
     *
     * @return the documents, in the order the mainframe lists them; empty if there are none
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public List<StoredDocument> documentsOfLoan(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        return call(() -> {
            List<StoredDocument> all = new ArrayList<>();
            for (long page = 1; page <= MAX_PAGES; page++) {
                PageResult<LoanDocument> result = loanDocuments.listDocuments(loanNumber, page, PAGE_SIZE);
                for (LoanDocument summary : result.getList()) {
                    all.add(fromLoan(loanDocuments.findDocument(loanNumber, summary.id()).orElse(summary)));
                }
                if (result.getList().isEmpty() || all.size() >= result.getTotal()) {
                    break;
                }
            }
            return all;
        });
    }

    /**
     * Records a document against a loan, with the same rules as {@link #addToCustomer}.
     *
     * @return the document as the mainframe recorded it
     * @throws ValidationException if the file name or kind is missing, or the file name is too long
     * @throws IllegalArgumentException if the size is negative
     * @throws RecordRejectedException if the mainframe did not accept the document
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public StoredDocument addToLoan(String loanNumber, String fileName, String fileType, long size, String type) {
        requireText(loanNumber, "loanNumber");
        Details details = checked(fileName, fileType, size, type);
        return fromLoan(call(() -> loanDocuments.addDocument(
                loanNumber, details.fileName(), details.fileType(), size, details.type())));
    }

    /**
     * Removes a document from a loan.
     *
     * @throws RecordRejectedException if the mainframe refused, for example because there is no such document
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public void removeFromLoan(String loanNumber, long documentId) {
        requireText(loanNumber, "loanNumber");
        call(() -> {
            loanDocuments.deleteDocument(loanNumber, documentId);
            return null;
        });
    }

    private record Details(String fileName, String fileType, String type) {
    }

    /** Tidies the details and reports every problem at once, by the field it belongs to. */
    private static Details checked(String fileName, String fileType, long size, String type) {
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        String name = fileName == null ? "" : fileName.strip();
        String kind = type == null ? "" : type.strip();
        String fileKind = fileType == null || fileType.isBlank() ? UNKNOWN_FILE_TYPE : fileType.strip();
        List<ValidationError> errors = new ArrayList<>();
        if (name.isEmpty()) {
            errors.add(new ValidationError(FILE_NAME_FIELD, "Choose a file to record"));
        } else if (name.length() > MAX_FILE_NAME_LENGTH) {
            errors.add(new ValidationError(FILE_NAME_FIELD,
                    "A file name can be at most " + MAX_FILE_NAME_LENGTH + " characters"));
        }
        if (kind.isEmpty()) {
            errors.add(new ValidationError(TYPE_FIELD, "Choose what kind of document this is"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new Details(name, fileKind, kind);
    }

    private static StoredDocument fromCustomer(String customerNumber, Document document) {
        String owner = document.getCustomerNumber() == null ? customerNumber : document.getCustomerNumber();
        Long size = document.getSize() > 0 ? document.getSize() : null;
        return new StoredDocument(document.getId(), owner, document.getFileName(), document.getFileType(), size,
                document.getType());
    }

    private static StoredDocument fromLoan(LoanDocument document) {
        return new StoredDocument(document.id(), document.loanNumber(), document.fileName(), document.fileType(),
                document.size(), document.type());
    }

    /**
     * Gives every "cannot reach the mainframe" the same message staff can act on. An
     * ended session keeps its own message, since only logging in again fixes it.
     */
    private static <T> T call(Call<T> call) {
        try {
            return call.run();
        } catch (IOException e) {
            throw new MainframeUnavailableException(OFFLINE_MESSAGE, e);
        } catch (SessionExpiredException e) {
            throw e;
        } catch (MainframeUnavailableException e) {
            throw new MainframeUnavailableException(OFFLINE_MESSAGE, e);
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
