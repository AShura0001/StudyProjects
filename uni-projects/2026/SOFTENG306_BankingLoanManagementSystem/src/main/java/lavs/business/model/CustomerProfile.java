package lavs.business.model;

/**
 * The few facts about a customer that the loan rules need.
 *
 * @param hasMailingAddress true if the customer has at least one address marked as a mailing address
 * @param hasPhoneOrEmail true if the customer has at least one phone number or email
 * @param hasEmployer true if the customer has at least one employer; false for someone without a job
 */
public record CustomerProfile(boolean hasMailingAddress, boolean hasPhoneOrEmail, boolean hasEmployer) {
}
