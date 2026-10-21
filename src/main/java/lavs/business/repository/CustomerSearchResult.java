package lavs.business.repository;

import lavs.business.model.CustomerType;

import java.util.List;

/**
 * One page of customer records. Kept so existing customer code does not need to
 * change; new code can use {@link PageResult} directly.
 *
 * @param <T> the kind of customer record on the page
 */
public class CustomerSearchResult<T extends CustomerType> extends PageResult<T> {

    public CustomerSearchResult(
            List<T> items,
            long total,
            long page
    ) {
        super(items, total, page);
    }
}
