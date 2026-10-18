package lavs.business.repository;

import lavs.business.model.CustomerType;

import java.util.List;

public class CustomerSearchResult<T extends CustomerType> {

    private final List<T> items;
    private final long total;
    private final long page;

    public CustomerSearchResult(
            List<T> items,
            long total,
            long page
    ) {
        this.items = List.copyOf(items);
        this.total = total;
        this.page = page;
    }

    public List<T> getList() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public long getPage() {
        return page;
    }
}