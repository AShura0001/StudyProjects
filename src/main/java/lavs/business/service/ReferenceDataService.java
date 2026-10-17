package lavs.business.service;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.ReferenceDataGateway;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Retrieves and caches the constant values for categories, such as the list of
 * address types.
 *
 * <p>Each category is fetched from the mainframe once and then read from the
 * cache, so values already fetched keep working while offline. Failed fetches
 * are not cached, so the next call tries the mainframe again.
 *
 * <p>Safe to call from several threads at once.
 */
public class ReferenceDataService {

    private static final Comparator<ReferenceItem> BY_ORDER_THEN_NAME =
            Comparator.comparingLong(ReferenceItem::order).thenComparing(ReferenceItem::name);

    private final ReferenceDataGateway gateway;
    private Map<String, Long> categoryIds;
    private final Map<String, List<String>> valuesByCategory = new HashMap<>();

    /**
     * Creates a service with an empty cache.
     *
     * @param gateway how to reach the mainframe
     * @throws NullPointerException if the gateway is null
     */
    public ReferenceDataService(ReferenceDataGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    }

    /**
     * Gets the values in a category, sorted the way the mainframe orders them.
     * Values with the same order are sorted by name so the list never shuffles.
     *
     * @param categoryName the category's name as the mainframe spells it, for
     *                     example "CustomerAddressType"
     * @return the value names, which cannot be modified
     * @throws NullPointerException if the category name is null
     * @throws IllegalArgumentException if the mainframe has no category with that name
     * @throws MainframeUnavailableException if the values are not cached and the
     *                                       mainframe could not be reached
     */
    public synchronized List<String> getValues(String categoryName) {
        Objects.requireNonNull(categoryName, "categoryName must not be null");

        List<String> cached = valuesByCategory.get(categoryName);
        if (cached != null) {
            return cached;
        }

        Long categoryId = categoryIds().get(categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Unknown reference data category: " + categoryName);
        }

        List<String> values = gateway.listItems(categoryId).stream()
                .sorted(BY_ORDER_THEN_NAME)
                .map(ReferenceItem::name)
                .toList();
        valuesByCategory.put(categoryName, values);
        return values;
    }

    /**
     * Empties the cache, so every category is fetched from the mainframe again
     * the next time it is asked for.
     */
    public synchronized void refresh() {
        categoryIds = null;
        valuesByCategory.clear();
    }

    private Map<String, Long> categoryIds() {
        if (categoryIds == null) {
            Map<String, Long> ids = new HashMap<>();
            for (ReferenceCategory category : gateway.listCategories()) {
                ids.put(category.name(), category.id());
            }
            categoryIds = ids;
        }
        return categoryIds;
    }
}
