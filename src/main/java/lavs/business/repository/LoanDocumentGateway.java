package lavs.business.repository;

import lavs.business.model.LoanDocument;

import java.util.Optional;

/**
 * Reads and changes the documents attached to a loan in the mainframe.
 *
 * <p>Every method throws {@link MainframeUnavailableException} if the mainframe
 * could not be reached.
 */
public interface LoanDocumentGateway {

    /**
     * Gets one document on a loan, including its file type and size.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the document on the loan
     * @return the document, or empty if there is no such document
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    Optional<LoanDocument> findDocument(String loanNumber, long id);

    /**
     * Gets one page of the documents on a loan. The mainframe does not send the
     * file type or size in a list, so those are null.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param page the page to get, starting at 1
     * @param pageSize how many documents to put on a page
     * @return the page; empty if the loan has none or does not exist
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    PageResult<LoanDocument> listDocuments(String loanNumber, long page, long pageSize);

    /**
     * Attaches a document to a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param fileName the name of the file
     * @param fileType the kind of file, for example {@code application/pdf}
     * @param size the size of the file in bytes
     * @param type what the document is
     * @return the document as the mainframe saved it
     * @throws RecordRejectedException if the mainframe refused the document
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    LoanDocument addDocument(String loanNumber, String fileName, String fileType, long size, String type);

    /**
     * Removes a document from a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the document on the loan
     * @throws RecordRejectedException if the mainframe refused, for example because there is no such document
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    void deleteDocument(String loanNumber, long id);
}
