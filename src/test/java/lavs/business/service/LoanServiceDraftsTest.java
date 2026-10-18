package lavs.business.service;

import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.model.LoanPayloads.CoBorrowerContent;
import lavs.business.model.LoanPayloads.LoanContent;
import lavs.business.repository.DraftStorageException;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.repository.FakeDraftUploadGateway.Attempt;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Creating loans and co-borrowers: they are saved as drafts, so they work offline. */
class LoanServiceDraftsTest {

    private static final LoanDetails DETAILS = new LoanDetails(new BigDecimal("250000.00"), "Fixed",
            new BigDecimal("5.85"), "Yearly", new BigDecimal("1450.00"), "Monthly",
            LocalDate.of(2026, 10, 1), LocalDate.of(2031, 10, 1));
    private static final CustomerProfile COMPLETE = new CustomerProfile(true, true, true);
    private static final CustomerReference EXISTING = CustomerReference.existing("01-0001-0000020");

    private final FakeDraftRepository repository = new FakeDraftRepository();
    private final FakeDraftUploadGateway mainframe = new FakeDraftUploadGateway();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final BackgroundSync sync = new BackgroundSync(new DraftSyncService(repository, mainframe),
            scheduler, RetryPolicy.standard());
    private final CustomerProfileSource profiles = mock(CustomerProfileSource.class);
    private final Map<String, String> validatorProblems = new LinkedHashMap<>();
    private final LoanValidator validator = details -> validatorProblems;
    private final LoanService service =
            new LoanService(mock(LoanGateway.class), profiles, validator, sync, repository);

    private void everyCustomerHas(CustomerProfile profile) {
        when(profiles.profileOf(any())).thenReturn(Optional.of(profile));
    }

    private CustomerReference savedCustomerDraft() {
        return CustomerReference.draft(sync.saveDraft(DraftType.CUSTOMER, "customer text").getLocalId());
    }

    private Draft stored(Draft draft) {
        return repository.findById(draft.getLocalId()).orElseThrow();
    }

    private LoanRuleException problemsFrom(CustomerReference customer) {
        return assertThrows(LoanRuleException.class, () -> service.createLoan(customer, DETAILS));
    }

    @Test
    void newService_withANullCollaborator_isRejected() {
        LoanGateway gateway = mock(LoanGateway.class);
        assertThrows(NullPointerException.class, () -> new LoanService(null, profiles, validator, sync, repository));
        assertThrows(NullPointerException.class, () -> new LoanService(gateway, null, validator, sync, repository));
        assertThrows(NullPointerException.class, () -> new LoanService(gateway, profiles, null, sync, repository));
        assertThrows(NullPointerException.class, () -> new LoanService(gateway, profiles, validator, null, repository));
        assertThrows(NullPointerException.class, () -> new LoanService(gateway, profiles, validator, sync, null));
    }

    @Test
    void createLoan_forAnExistingCustomer_savesAPendingLoanDraftThatDependsOnNothing() {
        everyCustomerHas(COMPLETE);

        Draft draft = service.createLoan(EXISTING, DETAILS);

        assertEquals(DraftType.LOAN, stored(draft).getType());
        assertEquals(DraftStatus.PENDING, stored(draft).getStatus());
        assertTrue(stored(draft).getDependsOn().isEmpty());
        LoanContent content = LoanPayloads.readLoan(stored(draft).getPayload());
        assertEquals(EXISTING, content.customer());
        assertEquals(DETAILS, content.details());
    }

    @Test
    void createLoan_forADraftCustomer_makesTheLoanDependOnThatCustomersDraft() {
        everyCustomerHas(COMPLETE);
        CustomerReference customer = savedCustomerDraft();

        Draft draft = service.createLoan(customer, DETAILS);

        assertEquals(List.of(((CustomerReference.DraftCustomer) customer).draftId()), stored(draft).getDependsOn());
        assertEquals(customer, LoanPayloads.readLoan(stored(draft).getPayload()).customer());
    }

    @Test
    void createLoan_whenTheMainframeIsOffline_stillSavesTheLoanAndAsksForItToBeSent() {
        everyCustomerHas(COMPLETE);
        mainframe.goOffline();

        Draft draft = service.createLoan(EXISTING, DETAILS);

        assertEquals(DraftStatus.PENDING, stored(draft).getStatus());
        assertTrue(mainframe.attempts().isEmpty());
        assertEquals(1, scheduler.immediateCount());
    }

    @Test
    void createLoan_withoutAMailingAddress_isRefusedAndNothingIsSaved() {
        everyCustomerHas(new CustomerProfile(false, true, true));

        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(Map.of(LoanService.MAILING_ADDRESS_FIELD, LoanService.MAILING_ADDRESS_MESSAGE),
                problem.getProblems());
        assertTrue(repository.findPending().isEmpty());
        assertEquals(0, scheduler.immediateCount());
    }

    @Test
    void createLoan_withoutAPhoneOrEmail_isRefused() {
        everyCustomerHas(new CustomerProfile(true, false, true));

        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(Map.of(LoanService.CONTACT_METHOD_FIELD, LoanService.CONTACT_METHOD_MESSAGE),
                problem.getProblems());
    }

    @Test
    void createLoan_withoutAnEmployer_isStillAllowedBecauseAGuarantorCanBeAddedLater() {
        everyCustomerHas(new CustomerProfile(true, true, false));

        Draft draft = service.createLoan(EXISTING, DETAILS);

        assertEquals(DraftStatus.PENDING, stored(draft).getStatus());
    }

    @Test
    void createLoan_withNeitherAddressNorContact_reportsBothProblemsAtOnce() {
        everyCustomerHas(new CustomerProfile(false, false, false));

        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(List.of(LoanService.MAILING_ADDRESS_FIELD, LoanService.CONTACT_METHOD_FIELD),
                List.copyOf(problem.getProblems().keySet()));
    }

    @Test
    void createLoan_withValuesTheValidatorRefuses_reportsThemByFieldName() {
        everyCustomerHas(COMPLETE);
        validatorProblems.put("principal", "Must be greater than zero.");
        validatorProblems.put("renewalDate", "Must be after the start date.");

        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(validatorProblems, problem.getProblems());
        assertTrue(repository.findPending().isEmpty());
    }

    @Test
    void createLoan_withCustomerProblemsAndValuesProblems_reportsAllOfThemCustomerFirst() {
        everyCustomerHas(new CustomerProfile(false, true, true));
        validatorProblems.put("principal", "Must be greater than zero.");

        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(List.of(LoanService.MAILING_ADDRESS_FIELD, "principal"),
                List.copyOf(problem.getProblems().keySet()));
    }

    @Test
    void createLoan_whenTheValidatorUsesTheSameKeyAsACustomerProblem_keepsTheCustomerProblem() {
        everyCustomerHas(new CustomerProfile(false, true, true));
        validatorProblems.put(LoanService.MAILING_ADDRESS_FIELD, "something else");

        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(LoanService.MAILING_ADDRESS_MESSAGE, problem.getProblems().get(LoanService.MAILING_ADDRESS_FIELD));
    }

    @Test
    void createLoan_forACustomerThatCannotBeFound_isRefusedWithAGeneralProblem() {
        LoanRuleException problem = problemsFrom(EXISTING);

        assertEquals(Map.of(LoanRuleException.GENERAL, LoanService.CUSTOMER_NOT_FOUND_MESSAGE), problem.getProblems());
    }

    @Test
    void createLoan_whenTheCustomerCannotBeLookedUpBecauseTheMainframeIsDown_passesThatOnAndSavesNothing() {
        when(profiles.profileOf(any())).thenThrow(new MainframeUnavailableException("down"));

        assertThrows(MainframeUnavailableException.class, () -> service.createLoan(EXISTING, DETAILS));

        assertTrue(repository.findPending().isEmpty());
    }

    @Test
    void createLoan_forADraftThatWasNeverSaved_isRejectedAndNothingIsSaved() {
        everyCustomerHas(COMPLETE);

        assertThrows(IllegalArgumentException.class,
                () -> service.createLoan(CustomerReference.draft("ghost"), DETAILS));

        assertTrue(repository.findPending().isEmpty());
    }

    @Test
    void createLoan_whenTheStoreFails_reportsTheError() {
        everyCustomerHas(COMPLETE);
        repository.setFailingToSave(true);

        assertThrows(DraftStorageException.class, () -> service.createLoan(EXISTING, DETAILS));
    }

    @Test
    void createLoan_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> service.createLoan(null, DETAILS));
        assertThrows(NullPointerException.class, () -> service.createLoan(EXISTING, null));
    }

    @Test
    void addCoBorrower_forAnExistingCustomer_savesADraftThatDependsOnlyOnTheLoan() {
        everyCustomerHas(COMPLETE);
        Draft loan = service.createLoan(EXISTING, DETAILS);
        CustomerReference partner = CustomerReference.existing("01-0001-0000031");

        Draft draft = service.addCoBorrower(loan.getLocalId(), CoBorrowerType.COBORROWER, partner);

        assertEquals(DraftType.COBORROWER, stored(draft).getType());
        assertEquals(List.of(loan.getLocalId()), stored(draft).getDependsOn());
        CoBorrowerContent content = LoanPayloads.readCoBorrower(stored(draft).getPayload());
        assertEquals(CoBorrowerType.COBORROWER, content.type());
        assertEquals(partner, content.customer());
    }

    @Test
    void addCoBorrower_forADraftCustomer_dependsOnTheLoanFirstAndThenTheCustomer() {
        everyCustomerHas(COMPLETE);
        Draft loan = service.createLoan(EXISTING, DETAILS);
        CustomerReference partner = savedCustomerDraft();

        Draft draft = service.addCoBorrower(loan.getLocalId(), CoBorrowerType.GUARANTOR, partner);

        assertEquals(List.of(loan.getLocalId(), ((CustomerReference.DraftCustomer) partner).draftId()),
                stored(draft).getDependsOn());
        assertEquals(CoBorrowerType.GUARANTOR, LoanPayloads.readCoBorrower(stored(draft).getPayload()).type());
    }

    @Test
    void addCoBorrower_whenTheCustomerOwnsTheLoan_isRefused() {
        everyCustomerHas(COMPLETE);
        Draft loan = service.createLoan(EXISTING, DETAILS);

        LoanRuleException problem = assertThrows(LoanRuleException.class,
                () -> service.addCoBorrower(loan.getLocalId(), CoBorrowerType.GUARANTOR, EXISTING));

        assertEquals(Map.of(LoanService.CO_BORROWER_FIELD, LoanService.OWN_LOAN_MESSAGE), problem.getProblems());
        assertEquals(1, repository.findPending().size());
    }

    @Test
    void addCoBorrower_whenADraftCustomerOwnsTheLoan_isRefusedForThatSameDraft() {
        everyCustomerHas(COMPLETE);
        CustomerReference owner = savedCustomerDraft();
        Draft loan = service.createLoan(owner, DETAILS);

        assertThrows(LoanRuleException.class,
                () -> service.addCoBorrower(loan.getLocalId(), CoBorrowerType.COBORROWER, owner));
    }

    @Test
    void addCoBorrower_forADifferentDraftCustomerThanTheOwner_isAllowed() {
        everyCustomerHas(COMPLETE);
        Draft loan = service.createLoan(savedCustomerDraft(), DETAILS);

        Draft draft = service.addCoBorrower(loan.getLocalId(), CoBorrowerType.COBORROWER, savedCustomerDraft());

        assertEquals(DraftType.COBORROWER, stored(draft).getType());
    }

    @Test
    void addCoBorrower_forALoanThatCannotBeFound_isRefusedWithAGeneralProblem() {
        LoanRuleException problem = assertThrows(LoanRuleException.class,
                () -> service.addCoBorrower("ghost", CoBorrowerType.COBORROWER, EXISTING));

        assertEquals(Map.of(LoanRuleException.GENERAL, LoanService.LOAN_NOT_FOUND_MESSAGE), problem.getProblems());
    }

    @Test
    void addCoBorrower_toADraftThatIsNotALoan_isRejected() {
        Draft customer = sync.saveDraft(DraftType.CUSTOMER, "customer text");

        assertThrows(IllegalArgumentException.class,
                () -> service.addCoBorrower(customer.getLocalId(), CoBorrowerType.COBORROWER, EXISTING));
    }

    @Test
    void addCoBorrower_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> service.addCoBorrower(null, CoBorrowerType.GUARANTOR, EXISTING));
        assertThrows(NullPointerException.class, () -> service.addCoBorrower("x", null, EXISTING));
        assertThrows(NullPointerException.class, () -> service.addCoBorrower("x", CoBorrowerType.GUARANTOR, null));
    }

    @Test
    void aWholeApplication_isSentInTheRightOrderWithTheRightNumbers() {
        everyCustomerHas(COMPLETE);
        CustomerReference applicant = savedCustomerDraft();
        Draft loan = service.createLoan(applicant, DETAILS);
        CustomerReference partner = savedCustomerDraft();
        Draft attachment = service.addCoBorrower(loan.getLocalId(), CoBorrowerType.COBORROWER, partner);
        String applicantId = ((CustomerReference.DraftCustomer) applicant).draftId();
        String partnerId = ((CustomerReference.DraftCustomer) partner).draftId();

        scheduler.runImmediate();

        Map<String, String> numberOf = new HashMap<>();
        for (Attempt attempt : mainframe.attempts()) {
            numberOf.put(attempt.draftId(), attempt.assignedNumber());
        }
        List<String> order = mainframe.acceptedDraftIds();
        assertEquals(4, order.size());
        assertTrue(order.indexOf(applicantId) < order.indexOf(loan.getLocalId()));
        assertTrue(order.indexOf(loan.getLocalId()) < order.indexOf(attachment.getLocalId()));
        assertTrue(order.indexOf(partnerId) < order.indexOf(attachment.getLocalId()));
        Attempt loanAttempt = mainframe.attempts().get(order.indexOf(loan.getLocalId()));
        Attempt attachmentAttempt = mainframe.attempts().get(order.indexOf(attachment.getLocalId()));
        assertEquals(List.of(numberOf.get(applicantId)), loanAttempt.parentNumbers());
        assertEquals(List.of(numberOf.get(loan.getLocalId()), numberOf.get(partnerId)),
                attachmentAttempt.parentNumbers());
    }
}
