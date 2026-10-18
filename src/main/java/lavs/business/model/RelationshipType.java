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
}
