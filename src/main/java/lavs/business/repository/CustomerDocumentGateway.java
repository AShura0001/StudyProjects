package lavs.business.repository;

import lavs.business.model.Address;
import lavs.business.model.Document;

import java.io.IOException;
import java.util.Optional;

public interface CustomerDocumentGateway {
    /**
     *
     * @param id Document id
     * @param customerNumber
     * @return An Optional wrap of a Document if it exists otherwise the Optional is empty
     * @throws IOException
     */
    Optional<Document> findDocumentByCustomerNumberAndId(Long id, String customerNumber) throws IOException;


    /**
     *w
     * @param customerNumber
     * @param id Document id
     * @throws IOException
     */
    boolean deleteCustomerDocument(long id, String customerNumber) throws IOException;


    /**
     *
     * @param document
     * @return
     * @throws IOException
     */
    Optional<Document> addCustomerDocument(Document document) throws IOException;

    /**
     *
     * @param page  The current page of Customers to retrieve
     * @param customerNumber String search term
     * @param page_size Number of elements per page
     * @return
     * @throws IOException
     */
    CustomerSearchResult<Document> ListCustomerDocuments(long page, String customerNumber, long page_size) throws IOException;
}
