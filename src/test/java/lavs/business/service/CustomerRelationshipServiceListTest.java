package lavs.business.service;

import lavs.business.model.Relationship;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Listing every relationship of a customer through CustomerRelationshipService. */
class CustomerRelationshipServiceListTest {

    private final CustomerRelationshipGateway relationships = mock(CustomerRelationshipGateway.class);
    private final CustomerRelationshipService service =
            new CustomerRelationshipService(relationships, mock(CustomerGateway.class));

    private static Relationship summary(long id, String other) {
        Relationship relationship = new Relationship();
        relationship.set_id(id);
        relationship.set_customerNumber("C-1");
        relationship.set_secondaryCustomerNumber(other);
        relationship.set_type("Spouse");
        return relationship;
    }

    private static Relationship full(long id, String other) {
        return new Relationship("Jane Doe", "C-1", id, "", "Sam Lee", other, "Spouse");
    }

    @Test
    void relationshipsOf_readsEachRelationshipInFull() throws IOException {
        when(relationships.ListCustomerRelationships(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary(5, "C-2")), 1, 1));
        when(relationships.findRelationshipByCustomerNumberAndId(5, "C-1")).thenReturn(Optional.of(full(5, "C-2")));

        List<Relationship> found = service.relationshipsOf("C-1");

        assertEquals(1, found.size());
        assertEquals("Sam Lee", found.get(0).get_secondaryCustomerName());
    }

    @Test
    void relationshipsOf_keepsTheListVersionWhenOneCannotBeRead() throws IOException {
        Relationship listed = summary(5, "C-2");
        when(relationships.ListCustomerRelationships(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(listed), 1, 1));
        when(relationships.findRelationshipByCustomerNumberAndId(5, "C-1")).thenReturn(Optional.empty());

        assertEquals(List.of(listed), service.relationshipsOf("C-1"));
    }

    @Test
    void relationshipsOf_readsEveryPage() throws IOException {
        when(relationships.ListCustomerRelationships(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary(5, "C-2")), 2, 1));
        when(relationships.ListCustomerRelationships(eq(2L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary(6, "C-3")), 2, 2));
        when(relationships.findRelationshipByCustomerNumberAndId(anyLong(), eq("C-1"))).thenReturn(Optional.empty());

        List<Relationship> found = service.relationshipsOf("C-1");

        assertEquals(2, found.size());
        assertEquals("C-3", found.get(1).get_secondaryCustomerNumber());
    }

    @Test
    void relationshipsOf_aCustomerWithNone_isEmpty() throws IOException {
        when(relationships.ListCustomerRelationships(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 0, 1));

        assertTrue(service.relationshipsOf("C-1").isEmpty());
    }

    @Test
    void relationshipsOf_whenTheMainframeCannotBeReached_givesTheOfflineMessage() throws IOException {
        when(relationships.ListCustomerRelationships(anyLong(), eq("C-1"), anyLong()))
                .thenThrow(new IOException("socket closed"));

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.relationshipsOf("C-1"));
        assertEquals(CustomerRelationshipService.OFFLINE_MESSAGE, offline.getMessage());
    }

    @Test
    void relationshipsOf_withABlankCustomerNumber_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.relationshipsOf(" "));
        assertThrows(NullPointerException.class, () -> service.relationshipsOf(null));
    }
}
