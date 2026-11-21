package lavs.business.model;

import java.util.Objects;

/**
 * A document recorded on the mainframe against a customer or a loan. The mainframe
 * keeps the document's details (its file name, file type, size and what kind of
 * document it is), not the file itself.
 *
 * @param id the mainframe's id for the document
 * @param owner the customer number or loan number the document belongs to
 * @param fileName the name of the file, for example {@code payslip.pdf}
 * @param fileType the kind of file, for example {@code application/pdf}; null if not sent
 * @param size the size of the file in bytes; null if not sent
 * @param type what kind of document it is, for example {@code Proof of Identity}
 */
public record StoredDocument(long id, String owner, String fileName, String fileType, Long size, String type) {

    /**
     * Creates the document.
     *
     * @throws NullPointerException if the owner is null
     */
    public StoredDocument {
        Objects.requireNonNull(owner, "owner must not be null");
    }
}
