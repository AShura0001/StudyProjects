package lavs.business.service;

import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.repository.MainframeUnavailableException;

import java.util.Optional;

/**
 * What {@link LoanService} needs to know about a customer to apply the loan rules.
 *
 * <p>The customer service implements this. It has to work in both cases a
 * {@link CustomerReference} can describe:
 *
 * <ul>
 *   <li>{@link CustomerReference.ExistingCustomer}: the customer is on the
 *       mainframe, so the answer comes from the mainframe's addresses, phone
 *       numbers, emails and employers for that customer number.</li>
 *   <li>{@link CustomerReference.DraftCustomer}: the customer was added offline and
 *       is still only a saved draft, so the answer comes from the details saved in
 *       that draft. There is nothing on the mainframe to ask.</li>
 * </ul>
 */
public interface CustomerProfileSource {

    /**
     * Describes a customer for the loan rules.
     *
     * @param customer which customer, on the mainframe or still a draft
     * @return the customer's profile, or empty if there is no such customer
     * @throws MainframeUnavailableException if an existing customer has to be looked
     *                                       up on the mainframe and it could not be reached
     */
    Optional<CustomerProfile> profileOf(CustomerReference customer);
}
