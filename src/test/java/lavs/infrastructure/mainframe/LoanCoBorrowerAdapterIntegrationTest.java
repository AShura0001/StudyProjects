package lavs.infrastructure.mainframe;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class LoanCoBorrowerAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private LoanCoBorrowerAdapter adapter;
    private String loanNumber;
    private String coBorrowerNumber;

    @BeforeAll
    void setUpLoan() throws Exception {
        adapter = new LoanCoBorrowerAdapter(connection);
        loanNumber = findOrCreateTestLoan(CUSTOMER_NUMBER);
        coBorrowerNumber = findOtherCustomerNumber(CUSTOMER_NUMBER);
    }

    @Test
    void addFindListAndDeleteCoBorrower() throws Exception {
        CoBorrower added = adapter.addCoBorrower(loanNumber, coBorrowerNumber, CoBorrowerType.GUARANTOR);
        try {
            assertEquals(loanNumber, added.loanNumber());
            assertEquals(coBorrowerNumber, added.coBorrowerNumber());
            assertEquals(CoBorrowerType.GUARANTOR, added.type());

            CoBorrower found = adapter.findCoBorrower(loanNumber, added.id()).orElseThrow();
            assertEquals(added.id(), found.id());
            assertEquals(coBorrowerNumber, found.coBorrowerNumber());
            assertNotNull(found.coBorrowerName());

            List<CoBorrower> listed = fetchAllPages(
                    (page, size) -> adapter.listCoBorrowers(loanNumber, page, size));
            CoBorrower inList = listed.stream().filter(c -> c.id() == added.id()).findFirst()
                    .orElseThrow(() -> new AssertionError("Co-borrower " + added.id() + " was not in the list"));
            assertEquals(coBorrowerNumber, inList.coBorrowerNumber());
            assertEquals(CoBorrowerType.GUARANTOR, inList.type());
        } finally {
            adapter.deleteCoBorrower(loanNumber, added.id());
        }

        assertTrue(adapter.findCoBorrower(loanNumber, added.id()).isEmpty(), "Co-borrower should be deleted");
    }

    @Test
    void findUnknownCoBorrowerIsEmpty() {
        assertTrue(adapter.findCoBorrower(loanNumber, Long.MAX_VALUE).isEmpty());
    }

    @Test
    void deleteUnknownCoBorrowerIsRejected() {
        assertThrows(RecordRejectedException.class, () -> adapter.deleteCoBorrower(loanNumber, Long.MAX_VALUE));
    }

    @Test
    void listCoBorrowersOfUnknownLoanIsEmpty() {
        PageResult<CoBorrower> page = adapter.listCoBorrowers("99-9999-9999999-999", 1, 10);
        assertTrue(page.getList().isEmpty());
    }
}
