package lavs.business.model;

/**
 * Says which customer something belongs to, whether or not that customer is on
 * the mainframe yet.
 *
 * <p>A customer the staff member found on the mainframe has a number. A customer
 * they just added offline is only a {@link Draft} with a local ID, and gets its
 * number after it has been sent. Rules that compare two customers, such as "a
 * customer cannot be a co-borrower on their own loan", use this type so both
 * cases are handled the same way. Two references are equal only if they are the
 * same kind and hold the same text.
 */
public sealed interface CustomerReference {

    /**
     * Refers to a customer that already exists on the mainframe.
     *
     * @param customerNumber the mainframe's number for the customer
     * @return the reference
     * @throws NullPointerException if the number is null
     * @throws IllegalArgumentException if the number is blank
     */
    static CustomerReference existing(String customerNumber) {
        return new ExistingCustomer(customerNumber);
    }

    /**
     * Refers to a customer that is still only a draft.
     *
     * @param draftId the local ID of the customer's draft
     * @return the reference
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if the ID is blank
     */
    static CustomerReference draft(String draftId) {
        return new DraftCustomer(draftId);
    }

    /**
     * A customer that exists on the mainframe.
     *
     * @param customerNumber the mainframe's number for the customer
     */
    record ExistingCustomer(String customerNumber) implements CustomerReference {

        /**
         * Creates the reference.
         *
         * @throws NullPointerException if the number is null
         * @throws IllegalArgumentException if the number is blank
         */
        public ExistingCustomer {
            if (customerNumber == null) {
                throw new NullPointerException("customerNumber must not be null");
            }
            if (customerNumber.isBlank()) {
                throw new IllegalArgumentException("customerNumber must not be blank");
            }
        }
    }

    /**
     * A customer that is still only a draft.
     *
     * @param draftId the local ID of the customer's draft
     */
    record DraftCustomer(String draftId) implements CustomerReference {

        /**
         * Creates the reference.
         *
         * @throws NullPointerException if the ID is null
         * @throws IllegalArgumentException if the ID is blank
         */
        public DraftCustomer {
            if (draftId == null) {
                throw new NullPointerException("draftId must not be null");
            }
            if (draftId.isBlank()) {
                throw new IllegalArgumentException("draftId must not be blank");
            }
        }
    }
}
