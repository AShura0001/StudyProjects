package lavs.business.model;

import java.util.Objects;

/**
 * One constant value within a {@link ReferenceCategory}, such as "Home" for address types.
 *
 * @param name the value shown to the user
 * @param order where the value sits when the category's values are listed
 */
public record ReferenceItem(String name, long order) {

    /**
     * @throws NullPointerException if the name is null
     */
    public ReferenceItem {
        Objects.requireNonNull(name, "name must not be null");
    }
}
