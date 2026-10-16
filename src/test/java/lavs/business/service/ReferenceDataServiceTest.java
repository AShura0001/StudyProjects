package lavs.business.service;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.ReferenceDataGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    private static final String ADDRESS_TYPE = "CustomerAddressType";
    private static final String RATE_TYPE = "LoanRateType";

    @Mock
    private ReferenceDataGateway gateway;

    private ReferenceDataService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceDataService(gateway);
    }

    private void givenCategories() {
        when(gateway.listCategories()).thenReturn(List.of(
                new ReferenceCategory(2, ADDRESS_TYPE),
                new ReferenceCategory(7, RATE_TYPE)));
    }

    private void givenAddressTypes() {
        when(gateway.listItems(2)).thenReturn(List.of(
                new ReferenceItem("Postal", 2),
                new ReferenceItem("Residential", 1),
                new ReferenceItem("Other", 5)));
    }

    @Test
    void getValues_forKnownCategory_returnsValuesInMainframeOrder() {
        givenCategories();
        givenAddressTypes();

        assertEquals(List.of("Residential", "Postal", "Other"), service.getValues(ADDRESS_TYPE));
    }

    @Test
    void getValues_whenAskedTwice_fetchesFromMainframeOnce() {
        givenCategories();
        givenAddressTypes();

        service.getValues(ADDRESS_TYPE);
        List<String> second = service.getValues(ADDRESS_TYPE);

        assertEquals(List.of("Residential", "Postal", "Other"), second);
        verify(gateway, times(1)).listCategories();
        verify(gateway, times(1)).listItems(2);
    }

    @Test
    void getValues_forTwoCategories_listsCategoriesOnce() {
        givenCategories();
        givenAddressTypes();
        when(gateway.listItems(7)).thenReturn(List.of(new ReferenceItem("Fixed", 1)));

        service.getValues(ADDRESS_TYPE);
        service.getValues(RATE_TYPE);

        verify(gateway, times(1)).listCategories();
    }

    @Test
    void getValues_withTiedOrder_sortsByName() {
        givenCategories();
        when(gateway.listItems(2)).thenReturn(List.of(
                new ReferenceItem("debug_item_1", 1),
                new ReferenceItem("Residential", 1),
                new ReferenceItem("Postal", 2)));

        assertEquals(List.of("Residential", "debug_item_1", "Postal"), service.getValues(ADDRESS_TYPE));
    }

    @Test
    void getValues_forEmptyCategory_returnsEmptyList() {
        givenCategories();
        when(gateway.listItems(7)).thenReturn(List.of());

        assertEquals(List.of(), service.getValues(RATE_TYPE));
    }

    @Test
    void getValues_forUnknownCategory_isRejected() {
        givenCategories();

        assertThrows(IllegalArgumentException.class, () -> service.getValues("NoSuchType"));
        verify(gateway, never()).listItems(anyLong());
    }

    @Test
    void getValues_withNullCategory_isRejected() {
        assertThrows(NullPointerException.class, () -> service.getValues(null));
    }

    @Test
    void getValues_returnsListThatCannotBeChanged() {
        givenCategories();
        givenAddressTypes();

        List<String> values = service.getValues(ADDRESS_TYPE);

        assertThrows(UnsupportedOperationException.class, () -> values.add("Hacked"));
    }

    @Test
    void getValues_whenMainframeUnreachable_throwsAndRetriesNextTime() {
        givenCategories();
        when(gateway.listItems(2))
                .thenThrow(new MainframeUnavailableException("dropped"))
                .thenReturn(List.of(new ReferenceItem("Residential", 1)));

        assertThrows(MainframeUnavailableException.class, () -> service.getValues(ADDRESS_TYPE));
        assertEquals(List.of("Residential"), service.getValues(ADDRESS_TYPE));
        verify(gateway, times(2)).listItems(2);
    }

    @Test
    void getValues_whenCategoryListUnreachable_retriesNextTime() {
        when(gateway.listCategories())
                .thenThrow(new MainframeUnavailableException("dropped"))
                .thenReturn(List.of(new ReferenceCategory(2, ADDRESS_TYPE)));
        givenAddressTypes();

        assertThrows(MainframeUnavailableException.class, () -> service.getValues(ADDRESS_TYPE));
        assertEquals(List.of("Residential", "Postal", "Other"), service.getValues(ADDRESS_TYPE));
    }

    @Test
    void getValues_whenCachedAndMainframeGoesDown_stillReturnsValues() {
        givenCategories();
        when(gateway.listItems(2))
                .thenReturn(List.of(new ReferenceItem("Residential", 1)))
                .thenThrow(new MainframeUnavailableException("dropped"));

        service.getValues(ADDRESS_TYPE);

        assertEquals(List.of("Residential"), service.getValues(ADDRESS_TYPE));
    }

    @Test
    void refresh_afterValuesCached_fetchesFromMainframeAgain() {
        givenCategories();
        givenAddressTypes();
        service.getValues(ADDRESS_TYPE);

        service.refresh();
        service.getValues(ADDRESS_TYPE);

        verify(gateway, times(2)).listCategories();
        verify(gateway, times(2)).listItems(2);
    }

    @Test
    void newService_withNullGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new ReferenceDataService(null));
    }
}
