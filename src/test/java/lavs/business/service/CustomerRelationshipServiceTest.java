package lavs.business.service;

import lavs.business.model.Customer;
import lavs.business.model.Relationship;
import lavs.business.model.RelationshipDetails;
import lavs.business.model.RelationshipType;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerRelationshipServiceTest {

    private static final RelationshipDetails SPOUSE = new RelationshipDetails(RelationshipType.SPOUSE, "", "C-2");

    private final CustomerRelationshipGateway relationships = mock(CustomerRelationshipGateway.class);
    private final CustomerGateway customers = mock(CustomerGateway.class);
    private final CustomerRelationshipService service = new CustomerRelationshipService(relationships, customers);

    private void bothCustomersExist() throws IOException {
        when(customers.findByCustomerNumber(any())).thenReturn(Optional.of(new Customer()));
    }

    private void theMainframeSavesWhatItIsGiven() throws IOException {
        when(relationships.addCustomerRelationship(any())).thenAnswer(call -> Optional.of(call.getArgument(0)));
        when(relationships.updateCustomerRelationship(any())).thenAnswer(call -> Optional.of(call.getArgument(0)));
    }

    private Relationship existingRelationship() {
        return new Relationship("Jane Doe", "C-1", 7, "", "Sam Doe", "C-9", "Parent");
    }

    @Test
    void newService_withANullGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new CustomerRelationshipService(null, customers));
        assertThrows(NullPointerException.class, () -> new CustomerRelationshipService(relationships, null));
    }

    @Test
    void add_withValidDetails_savesWithTheMainframesNamesAndReturnsTheResult() throws IOException {
        bothCustomersExist();
        theMainframeSavesWhatItIsGiven();

        Relationship saved = service.add("C-1", SPOUSE);

        ArgumentCaptor<Relationship> sent = ArgumentCaptor.forClass(Relationship.class);
        verify(relationships).addCustomerRelationship(sent.capture());
        assertEquals("C-1", sent.getValue().get_customerNumber());
        assertEquals("C-2", sent.getValue().get_secondaryCustomerNumber());
        assertEquals("Spouse", sent.getValue().get_type());
        assertEquals("", sent.getValue().get_otherType());
        assertSame(sent.getValue(), saved);
    }

    @Test
    void add_withAnOtherRelationship_sendsTheDescriptionAsTheOtherType() throws IOException {
        bothCustomersExist();
        theMainframeSavesWhatItIsGiven();

        service.add("C-1", new RelationshipDetails(RelationshipType.OTHER, "family friend", "C-2"));

        ArgumentCaptor<Relationship> sent = ArgumentCaptor.forClass(Relationship.class);
        verify(relationships).addCustomerRelationship(sent.capture());
        assertEquals("Other", sent.getValue().get_type());
        assertEquals("family friend", sent.getValue().get_otherType());
    }

    @Test
    void add_withADescriptionOnATypeThatDoesNotUseOne_leavesItOut() throws IOException {
        bothCustomersExist();
        theMainframeSavesWhatItIsGiven();

        service.add("C-1", new RelationshipDetails(RelationshipType.PARENT, "ignored words", "C-2"));

        ArgumentCaptor<Relationship> sent = ArgumentCaptor.forClass(Relationship.class);
        verify(relationships).addCustomerRelationship(sent.capture());
        assertEquals("", sent.getValue().get_otherType());
    }

    @Test
    void add_toTheSameCustomer_isRefusedWithoutAskingTheMainframe() {
        RelationshipRuleException problem = assertThrows(RelationshipRuleException.class,
                () -> service.add("C-1", new RelationshipDetails(RelationshipType.SPOUSE, "", "C-1")));

        assertEquals(Map.of(CustomerRelationshipService.RELATED_CUSTOMER_FIELD,
                CustomerRelationshipService.SELF_MESSAGE), problem.getProblems());
        verifyNoInteractions(relationships, customers);
    }

    @Test
    void add_anOtherRelationshipWithoutADescription_isRefusedWithoutAskingTheMainframe() {
        RelationshipRuleException problem = assertThrows(RelationshipRuleException.class,
                () -> service.add("C-1", new RelationshipDetails(RelationshipType.OTHER, " ", "C-2")));

        assertEquals(Map.of(CustomerRelationshipService.DESCRIPTION_FIELD,
                CustomerRelationshipService.DESCRIPTION_MESSAGE), problem.getProblems());
        verifyNoInteractions(relationships, customers);
    }

    @Test
    void add_withTwoLocalProblems_reportsBothInOrder() {
        RelationshipRuleException problem = assertThrows(RelationshipRuleException.class,
                () -> service.add("C-1", new RelationshipDetails(RelationshipType.OTHER, "", "C-1")));

        assertEquals(List.of(CustomerRelationshipService.RELATED_CUSTOMER_FIELD,
                CustomerRelationshipService.DESCRIPTION_FIELD), List.copyOf(problem.getProblems().keySet()));
    }

    @Test
    void add_whenTheCustomerCannotBeFound_isRefusedAndNothingIsSaved() throws IOException {
        when(customers.findByCustomerNumber("C-1")).thenReturn(Optional.empty());
        when(customers.findByCustomerNumber("C-2")).thenReturn(Optional.of(new Customer()));

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.add("C-1", SPOUSE));

        assertEquals(Map.of(CustomerRelationshipService.CUSTOMER_FIELD,
                CustomerRelationshipService.CUSTOMER_NOT_FOUND_MESSAGE), problem.getProblems());
        verify(relationships, never()).addCustomerRelationship(any());
    }

    @Test
    void add_whenTheRelatedCustomerCannotBeFound_isRefusedAndNothingIsSaved() throws IOException {
        when(customers.findByCustomerNumber("C-1")).thenReturn(Optional.of(new Customer()));
        when(customers.findByCustomerNumber("C-2")).thenReturn(Optional.empty());

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.add("C-1", SPOUSE));

        assertEquals(Map.of(CustomerRelationshipService.RELATED_CUSTOMER_FIELD,
                CustomerRelationshipService.RELATED_CUSTOMER_NOT_FOUND_MESSAGE), problem.getProblems());
        verify(relationships, never()).addCustomerRelationship(any());
    }

    @Test
    void add_whenNeitherCustomerCanBeFound_reportsBoth() throws IOException {
        when(customers.findByCustomerNumber(any())).thenReturn(Optional.empty());

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.add("C-1", SPOUSE));

        assertEquals(List.of(CustomerRelationshipService.CUSTOMER_FIELD,
                CustomerRelationshipService.RELATED_CUSTOMER_FIELD), List.copyOf(problem.getProblems().keySet()));
    }

    @Test
    void add_whenTheMainframeDoesNotSaveIt_saysSo() throws IOException {
        bothCustomersExist();
        when(relationships.addCustomerRelationship(any())).thenReturn(Optional.empty());

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.add("C-1", SPOUSE));

        assertEquals(Map.of(RuleViolationException.GENERAL, CustomerRelationshipService.NOT_SAVED_MESSAGE),
                problem.getProblems());
    }

    @Test
    void add_whenTheConnectionFailsWhileLookingUpACustomer_givesAClearMessageAndKeepsTheCause() throws IOException {
        IOException cause = new IOException("connection reset");
        when(customers.findByCustomerNumber(any())).thenThrow(cause);

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> service.add("C-1", SPOUSE));

        assertEquals(CustomerRelationshipService.OFFLINE_MESSAGE, thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }

    @Test
    void add_whenTheConnectionFailsWhileSaving_givesAClearMessage() throws IOException {
        bothCustomersExist();
        when(relationships.addCustomerRelationship(any())).thenThrow(new IOException("timed out"));

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> service.add("C-1", SPOUSE));

        assertEquals(CustomerRelationshipService.OFFLINE_MESSAGE, thrown.getMessage());
    }

    @Test
    void add_withNullOrBlankInput_isRejectedWithoutAskingTheMainframe() {
        assertThrows(NullPointerException.class, () -> service.add(null, SPOUSE));
        assertThrows(NullPointerException.class, () -> service.add("C-1", null));
        assertThrows(IllegalArgumentException.class, () -> service.add(" ", SPOUSE));

        verifyNoInteractions(relationships, customers);
    }

    @Test
    void update_withValidDetails_keepsTheIdAndTheCustomerNameAndSendsTheNewValues() throws IOException {
        bothCustomersExist();
        theMainframeSavesWhatItIsGiven();
        when(relationships.findRelationshipByCustomerNumberAndId(7, "C-1")).thenReturn(Optional.of(existingRelationship()));

        Relationship saved = service.update("C-1", 7, new RelationshipDetails(RelationshipType.LEGAL_AUTHORITY, "", "C-3"));

        ArgumentCaptor<Relationship> sent = ArgumentCaptor.forClass(Relationship.class);
        verify(relationships).updateCustomerRelationship(sent.capture());
        assertEquals(7, sent.getValue().get_id());
        assertEquals("C-1", sent.getValue().get_customerNumber());
        assertEquals("Jane Doe", sent.getValue().get_customerName());
        assertEquals("C-3", sent.getValue().get_secondaryCustomerNumber());
        assertEquals("LegalAuthority", sent.getValue().get_type());
        assertSame(sent.getValue(), saved);
    }

    @Test
    void update_whenTheRelationshipCannotBeFound_isRefusedAndNothingIsChanged() throws IOException {
        when(relationships.findRelationshipByCustomerNumberAndId(anyLong(), any())).thenReturn(Optional.empty());

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.update("C-1", 7, SPOUSE));

        assertEquals(Map.of(RuleViolationException.GENERAL, CustomerRelationshipService.RELATIONSHIP_NOT_FOUND_MESSAGE),
                problem.getProblems());
        verify(relationships, never()).updateCustomerRelationship(any());
    }

    @Test
    void update_toTheSameCustomer_isRefusedWithoutAskingTheMainframe() {
        assertThrows(RelationshipRuleException.class,
                () -> service.update("C-1", 7, new RelationshipDetails(RelationshipType.SPOUSE, "", "C-1")));

        verifyNoInteractions(relationships, customers);
    }

    @Test
    void update_anOtherRelationshipWithoutADescription_isRefusedWithoutAskingTheMainframe() {
        assertThrows(RelationshipRuleException.class,
                () -> service.update("C-1", 7, new RelationshipDetails(RelationshipType.OTHER, "", "C-2")));

        verifyNoInteractions(relationships, customers);
    }

    @Test
    void update_whenTheRelatedCustomerCannotBeFound_isRefusedAndNothingIsChanged() throws IOException {
        when(relationships.findRelationshipByCustomerNumberAndId(7, "C-1")).thenReturn(Optional.of(existingRelationship()));
        when(customers.findByCustomerNumber("C-2")).thenReturn(Optional.empty());

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.update("C-1", 7, SPOUSE));

        assertEquals(Map.of(CustomerRelationshipService.RELATED_CUSTOMER_FIELD,
                CustomerRelationshipService.RELATED_CUSTOMER_NOT_FOUND_MESSAGE), problem.getProblems());
        verify(relationships, never()).updateCustomerRelationship(any());
    }

    @Test
    void update_whenTheMainframeDoesNotSaveIt_saysSo() throws IOException {
        bothCustomersExist();
        when(relationships.findRelationshipByCustomerNumberAndId(7, "C-1")).thenReturn(Optional.of(existingRelationship()));
        when(relationships.updateCustomerRelationship(any())).thenReturn(Optional.empty());

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.update("C-1", 7, SPOUSE));

        assertEquals(CustomerRelationshipService.NOT_SAVED_MESSAGE, problem.getMessage());
    }

    @Test
    void update_whenTheConnectionFails_givesAClearMessage() throws IOException {
        when(relationships.findRelationshipByCustomerNumberAndId(anyLong(), any())).thenThrow(new IOException("down"));

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> service.update("C-1", 7, SPOUSE));

        assertEquals(CustomerRelationshipService.OFFLINE_MESSAGE, thrown.getMessage());
    }

    @Test
    void update_withNullOrBlankInput_isRejected() {
        assertThrows(NullPointerException.class, () -> service.update(null, 7, SPOUSE));
        assertThrows(NullPointerException.class, () -> service.update("C-1", 7, null));
        assertThrows(IllegalArgumentException.class, () -> service.update("", 7, SPOUSE));
    }

    @Test
    void delete_whenTheMainframeConfirms_asksOnceWithTheIdAndCustomer() throws IOException {
        when(relationships.deleteRelationshipByCustomerNumberAndId(7, "C-1")).thenReturn(true);

        service.delete("C-1", 7);

        verify(relationships).deleteRelationshipByCustomerNumberAndId(eq(7L), eq("C-1"));
    }

    @Test
    void delete_whenTheMainframeDoesNotConfirm_saysSo() throws IOException {
        when(relationships.deleteRelationshipByCustomerNumberAndId(anyLong(), any())).thenReturn(false);

        RelationshipRuleException problem =
                assertThrows(RelationshipRuleException.class, () -> service.delete("C-1", 7));

        assertEquals(Map.of(RuleViolationException.GENERAL, CustomerRelationshipService.NOT_DELETED_MESSAGE),
                problem.getProblems());
    }

    @Test
    void delete_whenTheConnectionFails_givesAClearMessage() throws IOException {
        when(relationships.deleteRelationshipByCustomerNumberAndId(anyLong(), any())).thenThrow(new IOException("down"));

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> service.delete("C-1", 7));

        assertEquals(CustomerRelationshipService.OFFLINE_MESSAGE, thrown.getMessage());
    }

    @Test
    void delete_withNullOrBlankCustomer_isRejected() {
        assertThrows(NullPointerException.class, () -> service.delete(null, 7));
        assertThrows(IllegalArgumentException.class, () -> service.delete(" ", 7));
    }

    @Test
    void find_withAKnownRelationship_returnsIt() throws IOException {
        when(relationships.findRelationshipByCustomerNumberAndId(7, "C-1")).thenReturn(Optional.of(existingRelationship()));

        assertEquals("C-9", service.find("C-1", 7).orElseThrow().get_secondaryCustomerNumber());
    }

    @Test
    void find_withAnUnknownRelationship_returnsEmpty() throws IOException {
        when(relationships.findRelationshipByCustomerNumberAndId(anyLong(), any())).thenReturn(Optional.empty());

        assertTrue(service.find("C-1", 99).isEmpty());
    }

    @Test
    void find_whenTheConnectionFails_givesAClearMessage() throws IOException {
        when(relationships.findRelationshipByCustomerNumberAndId(anyLong(), any())).thenThrow(new IOException("down"));

        assertThrows(MainframeUnavailableException.class, () -> service.find("C-1", 7));
    }

    @Test
    void find_withNullOrBlankCustomer_isRejected() {
        assertThrows(NullPointerException.class, () -> service.find(null, 7));
        assertThrows(IllegalArgumentException.class, () -> service.find(" ", 7));
    }
}
