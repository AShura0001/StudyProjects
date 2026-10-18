package lavs.business.model;

import java.util.Objects;

/**
 * A document attached to a loan, as the mainframe holds it.
 *
 * <p>When the document comes from a list, the mainframe only sends the id, file
 * name and type, so the file type and size are null.
 *
 * @param id the mainframe's id for the document on the loan
 * @param loanNumber the mainframe's number for the loan
 * @param fileName the name of the file
 * @param fileType the kind of file, for example {@code application/pdf}; null if not sent
 * @param size the size of the file in bytes; null if not sent
 * @param type what the document is, for example proof of income
 */
public record LoanDocument(
        long id,
        String loanNumber,
        String fileName,
        String fileType,
        Long size,
        String type) {

    /**
     * Creates the document.
     *
     * @throws NullPointerException if the loan number, file name or type is null
     */
    public LoanDocument {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
