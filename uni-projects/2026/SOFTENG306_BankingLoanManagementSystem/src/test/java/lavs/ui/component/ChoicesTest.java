package lavs.ui.component;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import lavs.business.repository.ReferenceDataGateway;
import lavs.business.service.ReferenceDataService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChoicesTest {

    private static final List<String> FIXED = List.of("Fixed list");

    private final ReferenceDataGateway gateway = mock(ReferenceDataGateway.class);
    private final ReferenceDataService referenceData = new ReferenceDataService(gateway);

    private void givenFetched(List<ReferenceItem> items) {
        when(gateway.listCategories()).thenReturn(List.of(new ReferenceCategory(2, Choices.ADDRESS_TYPE_CATEGORY)));
        when(gateway.listItems(2)).thenReturn(items);
        referenceData.getValues(Choices.ADDRESS_TYPE_CATEGORY);
    }

    @Test
    void pick_whenTheCategoryHasBeenFetched_usesTheMainframesValues() {
        givenFetched(List.of(new ReferenceItem("Residential", 1), new ReferenceItem("Postal", 2)));

        assertEquals(List.of("Residential", "Postal"),
                Choices.pick(Optional.of(referenceData), Choices.ADDRESS_TYPE_CATEGORY, FIXED));
    }

    @Test
    void pick_whenTheCategoryHasNotBeenFetched_usesTheFixedListWithoutAskingTheMainframe() {
        assertEquals(FIXED, Choices.pick(Optional.of(referenceData), Choices.ADDRESS_TYPE_CATEGORY, FIXED));
        verifyNoInteractions(gateway);
    }

    @Test
    void pick_whenTheMainframeListIsEmpty_usesTheFixedList() {
        givenFetched(List.of());

        assertEquals(FIXED, Choices.pick(Optional.of(referenceData), Choices.ADDRESS_TYPE_CATEGORY, FIXED));
    }

    @Test
    void pick_withNoReferenceData_usesTheFixedList() {
        assertEquals(FIXED, Choices.pick(Optional.empty(), Choices.ADDRESS_TYPE_CATEGORY, FIXED));
    }

    @Test
    void lists_beforeLogin_areTheFixedLists() {
        assertEquals(Choices.ADDRESS_TYPES, Choices.addressTypes());
        assertEquals(Choices.RATE_TYPES, Choices.rateTypes());
        assertEquals(Choices.RATE_FREQUENCIES, Choices.rateFrequencies());
        assertEquals(Choices.PAYMENT_FREQUENCIES, Choices.paymentFrequencies());
        assertEquals(Choices.CO_BORROWER_TYPES, Choices.coBorrowerTypes());
        assertEquals(Choices.PHONE_TYPES, Choices.phoneTypes());
        assertEquals(Choices.EMAIL_TYPES, Choices.emailTypes());
    }

    @Test
    void fixedLists_matchTheMainframesSpellings() {
        // The values the mainframe lists for each category, so the fixed fallback sends the same words.
        assertEquals(List.of("Residential", "Postal", "Forwarding", "Delegated", "Other"), Choices.ADDRESS_TYPES);
        assertEquals(List.of("Fixed", "Floating", "InterestOnly"), Choices.RATE_TYPES);
        assertEquals(List.of("Weekly", "Fortnightly", "Monthly", "Yearly", "Onrenewal"), Choices.PAYMENT_FREQUENCIES);
        assertEquals(List.of("Coborrower", "Guarantor"), Choices.CO_BORROWER_TYPES);
        assertEquals(List.of("Home", "Mobile", "Work", "Other"), Choices.PHONE_TYPES);
        assertEquals(List.of("Personal", "Work", "Other"), Choices.EMAIL_TYPES);
    }
}
