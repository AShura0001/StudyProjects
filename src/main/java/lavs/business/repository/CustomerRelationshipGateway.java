package lavs.business.repository;

import lavs.business.model.PhoneNumber;
import lavs.business.model.Relationship;

import java.io.IOException;
import java.util.Optional;


public interface CustomerRelationshipGateway {

    /**
     *
     * @param id
     * @param customerNumber
     * @return An Optional wrap of the relationship if it exists, otherwise an Optional.empty()
     */
    Optional<Relationship> findRelationshipByCustomerNumberAndId(long id, String customerNumber) throws IOException;

    /**
     * This method only updates relationship individually
     * @param relationship and instance of the relationship
     * @return An Optional wrap of the relationship when retrieved, an Optional.empty() when otherwise
     */
    Optional<Relationship> updateCustomerRelationship(Relationship relationship) throws IOException;

    /**
     *
     * @param id
     * @param customerNumber
     * @return true if successful
     */
    boolean deleteRelationshipByCustomerNumberAndId(long id, String customerNumber) throws IOException;

    /**
     *
     * @param relationship
     * @return
     * @throws IOException
     */
    Optional<Relationship> addCustomerRelationship(Relationship relationship) throws IOException;

    /**
     *
     * @param page  The current page of Relationships to retrieve
     * @param customerNumber The customer whose Relationships are listed
     * @param page_size Number of elements per page
     * @return A page of Relationships for the customer, empty if the request failed
     * @throws IOException
     */
    CustomerSearchResult<Relationship> ListCustomerRelationships(long page, String customerNumber, long page_size) throws IOException;
}
