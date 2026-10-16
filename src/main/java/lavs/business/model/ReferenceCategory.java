package lavs.business.model;

import java.util.Objects;

/**
 * A group of constant values on the mainframe, such as address types or rate types.
 *
 * @param id the mainframe's ID for the category, used to fetch its values
 * @param name the category's name, for example "Address Type"
 */
public record ReferenceCategory(long id, String name) {

    /**
     * @throws NullPointerException if the name is null
     */
    public ReferenceCategory {
        Objects.requireNonNull(name, "name must not be null");
    }
}
