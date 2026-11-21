package lavs.business.model;

/**
 * The kinds of relationship between two customers that the mainframe accepts.
 */
public enum RelationshipType {

    /** One customer is the parent of the other. */
    PARENT("Parent"),

    /** The customers are spouses. */
    SPOUSE("Spouse"),

    /** One customer has legal authority over the other. */
    LEGAL_AUTHORITY("LegalAuthority"),

    /** Any other kind, which must be described in words. */
    OTHER("Other");

    private final String mainframeName;

    RelationshipType(String mainframeName) {
        this.mainframeName = mainframeName;
    }

    /**
     * Gets the name the mainframe uses for this type.
     *
     * @return the mainframe's name, for example {@code LegalAuthority}
     */
    public String mainframeName() {
        return mainframeName;
    }

    /**
     * Gets how this type reads to a person, for example "Legal authority".
     *
     * @return the label
     */
    public String label() {
        return this == LEGAL_AUTHORITY ? "Legal authority" : mainframeName;
    }

    /**
     * Finds the type the mainframe calls by the given name, ignoring case.
     *
     * @param name the mainframe's name, for example {@code LegalAuthority}
     * @return the type, or empty if the name is not one of the known types
     */
    public static java.util.Optional<RelationshipType> fromMainframeName(String name) {
        for (RelationshipType type : values()) {
            if (type.mainframeName.equalsIgnoreCase(name == null ? "" : name.strip())) {
                return java.util.Optional.of(type);
            }
        }
        return java.util.Optional.empty();
    }
}
