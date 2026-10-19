package lavs.business.repository;

import lavs.business.model.Employer;
import lavs.business.model.Note;

import java.io.IOException;
import java.util.Optional;

public interface CustomerNoteGateway {

    /**
     *
     * @param id             Note id
     * @param customerNumber
     * @return An Optional wrap of Note if it exists, otherwise Optional is empty
     * @throws IOException
     */
    Optional<Note> findNoteByCustomerNumberAndId(long id, String customerNumber) throws IOException;

    /**
     *
     * @param note An instance of the updated Note
     * @return An Optional wrap of the updated note if successful, empty if otherwise
     * @throws IOException
     */
    Optional<Note> updateCustomerNote(Note note) throws IOException;

    /**
     *
     * @param id             Note id
     * @param customerNumber
     * @throws IOException
     */
    boolean deleteNoteByCustomerNumberAndId(long id, String customerNumber) throws IOException;

    /**
     *
     * @param note
     * @return
     * @throws IOException
     */
    Optional<Note> addCustomerNote(Note note) throws IOException;

    /**
     *
     * @param page  The current page of Notes to retrieve
     * @param customerNumber The customer whose Notes are listed
     * @param page_size Number of elements per page
     * @return A page of Notes for the customer, empty if the request failed
     * @throws IOException
     */
    CustomerSearchResult<Note> ListCustomerNotes(long page, String customerNumber, long page_size) throws IOException;
}
