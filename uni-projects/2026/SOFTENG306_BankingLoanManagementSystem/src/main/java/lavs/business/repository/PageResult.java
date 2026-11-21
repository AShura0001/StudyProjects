package lavs.business.repository;

import java.util.List;
import java.util.Objects;

/**
 * One page of records returned by a list call to the mainframe.
 *
 * @param <T> the kind of record on the page
 */
public class PageResult<T> {

    private final List<T> items;
    private final long total;
    private final long page;

    /**
     * Creates a page.
     *
     * @param items the records on this page
     * @param total how many records there are across all pages
     * @param page the number of this page, starting at 1
     * @throws NullPointerException if the items, or any item, is null
     */
    public PageResult(List<T> items, long total, long page) {
        Objects.requireNonNull(items, "items must not be null");
        this.items = List.copyOf(items);
        this.total = total;
        this.page = page;
    }

    /**
     * Creates a page with no records, for example when the mainframe refused the request.
     *
     * @param page the number of the page that was asked for
     * @param <T> the kind of record on the page
     * @return an empty page with a total of 0
     */
    public static <T> PageResult<T> empty(long page) {
        return new PageResult<>(List.of(), 0, page);
    }

    /**
     * Gets the records on this page.
     *
     * @return an unmodifiable list of the records
     */
    public List<T> getList() {
        return items;
    }

    /**
     * Gets how many records there are across all pages.
     *
     * @return the total number of records
     */
    public long getTotal() {
        return total;
    }

    /**
     * Gets the number of this page.
     *
     * @return the page number, starting at 1
     */
    public long getPage() {
        return page;
    }
}
