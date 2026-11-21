package lavs.business.service;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.ReferenceDataGateway;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retrieves and caches the constant values for categories, such as the list of
 * address types.
 *
 * <p>Each category is fetched from the mainframe once and then read from the
 * cache, so values already fetched keep working while offline. Failed fetches
 * are not cached, so the next call tries the mainframe again.
 *
 * <p>Safe to call from several threads at once. {@link #cachedValues(String)}
 * never waits for the mainframe, so screens can call it while another thread
 * is fetching with {@link #preload(Collection)}.
 */
public class ReferenceDataService {

    private static final Comparator<ReferenceItem> BY_ORDER_THEN_NAME =
            Comparator.comparingLong(ReferenceItem::order).thenComparing(ReferenceItem::name);

    private final ReferenceDataGateway gateway;
    private Map<String, Long> categoryIds;
    private final Map<String, List<String>> valuesByCategory = new ConcurrentHashMap<>();

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
     * Gets the values in a category if they have already been fetched, without
     * asking the mainframe.
     *
     * @param categoryName the category's name as the mainframe spells it
     * @return the value names, or empty if the category has not been fetched yet
     * @throws NullPointerException if the category name is null
     */
    public Optional<List<String>> cachedValues(String categoryName) {
        Objects.requireNonNull(categoryName, "categoryName must not be null");
        return Optional.ofNullable(valuesByCategory.get(categoryName));
    }

    /**
     * Fetches several categories into the cache, so they are ready before they
     * are needed. A category that cannot be fetched, because the mainframe could
     * not be reached or has no category with that name, is skipped and will be
     * tried again next time.
     *
     * @param categoryNames the categories' names as the mainframe spells them
     * @return how many of the categories are now cached
     * @throws NullPointerException if the collection or any name in it is null
     */
    public int preload(Collection<String> categoryNames) {
        Objects.requireNonNull(categoryNames, "categoryNames must not be null");
        int loaded = 0;
        for (String categoryName : categoryNames) {
            try {
                getValues(categoryName);
                loaded++;
            } catch (MainframeUnavailableException | IllegalArgumentException e) {
                // Skipped; the next call tries the mainframe again.
            }
        }
        return loaded;
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
