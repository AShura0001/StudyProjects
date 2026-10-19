package lavs.infrastructure.mainframe;

import lavs.business.model.CoBorrowerType;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoanCoBorrowerAdapterTest {

    @Test
    void typesMapBothWays() {
        for (CoBorrowerType type : CoBorrowerType.values()) {
            assertEquals(type, LoanCoBorrowerAdapter.toType(LoanCoBorrowerAdapter.toMainframeType(type)));
        }
        assertEquals("Coborrower", LoanCoBorrowerAdapter.toMainframeType(CoBorrowerType.COBORROWER));
        assertEquals("Guarantor", LoanCoBorrowerAdapter.toMainframeType(CoBorrowerType.GUARANTOR));
    }

    @Test
    void toType_ignoresCase() {
        assertEquals(CoBorrowerType.GUARANTOR, LoanCoBorrowerAdapter.toType("GUARANTOR"));
    }

    @Test
    void toType_withUnknownType_throws() {
        assertThrows(RecordRejectedException.class, () -> LoanCoBorrowerAdapter.toType("Spouse"));
    }
}
