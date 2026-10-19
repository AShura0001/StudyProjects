package lavs.business.repository;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void newPageResult_keepsValues() {
        PageResult<String> page = new PageResult<>(List.of("a", "b"), 5, 2);

        assertEquals(List.of("a", "b"), page.getList());
        assertEquals(5, page.getTotal());
        assertEquals(2, page.getPage());
    }

    @Test
    void newPageResult_copiesItems() {
        List<String> items = new ArrayList<>(List.of("a"));
        PageResult<String> page = new PageResult<>(items, 1, 1);

        items.add("b");

        assertEquals(List.of("a"), page.getList());
        assertThrows(UnsupportedOperationException.class, () -> page.getList().add("c"));
    }

    @Test
    void newPageResult_withNullItems_isRejected() {
        assertThrows(NullPointerException.class, () -> new PageResult<String>(null, 0, 1));
    }

    @Test
    void empty_hasNoItemsAndZeroTotal() {
        PageResult<String> page = PageResult.empty(3);

        assertTrue(page.getList().isEmpty());
        assertEquals(0, page.getTotal());
        assertEquals(3, page.getPage());
    }
}
