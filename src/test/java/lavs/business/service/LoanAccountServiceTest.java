package lavs.business.service;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanRecord;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoanAccountServiceTest {

    private static final LoanRecord LOAN = new LoanRecord("LN-1", "CUS-1", "Jane Doe", "Active",
            new BigDecimal("250000"), "Fixed", new BigDecimal("5.85"), "Yearly",
            new BigDecimal("1450"), "Monthly", LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1));

    private LoanGateway loans;
    private LoanCoBorrowerGateway coBorrowers;
    private LoanPaymentGateway payments;
    private LoanAccountService service;

    @BeforeEach
    void setUp() {
        loans = mock(LoanGateway.class);
        coBorrowers = mock(LoanCoBorrowerGateway.class);
        payments = mock(LoanPaymentGateway.class);
        service = new LoanAccountService(loans, coBorrowers, payments, new BasicLoanValidator());
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(LOAN));
    }

    @Test
    void updateTerms_sendsValidTermsForTheLoansOwner() {
        LoanDetails terms = LoanAccountService.termsOf(LOAN).orElseThrow();
        LoanDetails changed = new LoanDetails(terms.principal(), terms.rateType(), terms.rateAmount(),
                terms.rateFrequency(), new BigDecimal("1600"), "Fortnightly", terms.startDate(), terms.renewalDate());

        service.updateTerms("LN-1", changed);

        verify(loans).updateLoan("LN-1", "CUS-1", changed);
    }

    @Test
    void updateTerms_withBrokenRules_reportsThemAndSendsNothing() {
        LoanDetails terms = LoanAccountService.termsOf(LOAN).orElseThrow();
        LoanDetails broken = new LoanDetails(BigDecimal.ZERO, terms.rateType(), terms.rateAmount(),
                terms.rateFrequency(), terms.paymentAmount(), terms.paymentFrequency(),
                terms.startDate(), terms.startDate());

        LoanRuleException refused = assertThrows(LoanRuleException.class, () -> service.updateTerms("LN-1", broken));

        assertTrue(refused.getProblems().containsKey("principal"));
        assertTrue(refused.getProblems().containsKey("renewalDate"));
        verify(loans, never()).updateLoan(any(), any(), any());
    }

    @Test
    void updateTerms_ofAnUnknownLoan_isRefused() {
        LoanDetails terms = LoanAccountService.termsOf(LOAN).orElseThrow();
        assertThrows(LoanRuleException.class, () -> service.updateTerms("LN-404", terms));
    }

    @Test
    void addCoBorrower_refusesTheLoansOwner() {
        LoanRuleException refused = assertThrows(LoanRuleException.class,
                () -> service.addCoBorrower("LN-1", "cus-1", CoBorrowerType.COBORROWER));
        assertEquals(LoanService.OWN_LOAN_MESSAGE, refused.getProblems().get(LoanService.CO_BORROWER_FIELD));
        verify(coBorrowers, never()).addCoBorrower(any(), any(), any());
    }

    @Test
    void addCoBorrower_addsSomeoneElse() {
        CoBorrower added = new CoBorrower(7, "LN-1", "CUS-2", "Sam Lee", CoBorrowerType.GUARANTOR);
        when(coBorrowers.addCoBorrower("LN-1", "CUS-2", CoBorrowerType.GUARANTOR)).thenReturn(added);
        assertEquals(added, service.addCoBorrower("LN-1", "CUS-2", CoBorrowerType.GUARANTOR));
    }

    @Test
    void coBorrowersOf_readsEveryPage() {
        CoBorrower a = new CoBorrower(1, "LN-1", "CUS-2", "A", CoBorrowerType.COBORROWER);
        CoBorrower b = new CoBorrower(2, "LN-1", "CUS-3", "B", CoBorrowerType.GUARANTOR);
        when(coBorrowers.listCoBorrowers(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(List.of(a), 2, 1));
        when(coBorrowers.listCoBorrowers(eq("LN-1"), eq(2L), anyLong())).thenReturn(new PageResult<>(List.of(b), 2, 2));

        assertEquals(List.of(a, b), service.coBorrowersOf("LN-1"));
    }

    @Test
    void anUnreachableMainframe_givesAMessageStaffCanActOn() {
        when(loans.listLoans("CUS-1")).thenThrow(new MainframeUnavailableException("socket closed"));
        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.loansOf("CUS-1"));
        assertEquals(LoanAccountService.OFFLINE_MESSAGE, offline.getMessage());
    }

    @Test
    void termsOf_isEmptyWhenTheMainframeLeftATermOut() {
        LoanRecord partial = new LoanRecord("LN-2", "CUS-1", null, null, new BigDecimal("1"), "Fixed",
                new BigDecimal("1"), "Yearly", new BigDecimal("1"), "Monthly", null, LocalDate.of(2030, 1, 1));
        assertTrue(LoanAccountService.termsOf(partial).isEmpty());
    }
}
