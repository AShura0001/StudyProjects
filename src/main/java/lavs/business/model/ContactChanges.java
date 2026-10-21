package lavs.business.model;

import java.util.List;
import java.util.Objects;

/**
 * What a staff member wants a customer's main details to be: their name, their
 * primary phone and email, and the employers and addresses to take off their record.
 * Addresses are added and changed one at a time with CustomerService.addAddress/updateAddress.
 * Blank text means "none"; every value arrives trimmed.
 *
 * @param customerNumber the customer being changed
 * @param firstName the first name
 * @param familyName the family name
 * @param phoneCountryCode the primary phone's country code, such as {@code +64}; null to keep the current code
 * @param phoneNumber the primary phone number without its country code
 * @param email the primary email address
 * @param removedEmployerIds the employers to delete
 * @param removedAddressIds the addresses to delete
 */
public record ContactChanges(
        String customerNumber,
        String firstName,
        String familyName,
        String phoneCountryCode,
        String phoneNumber,
        String email,
        List<Long> removedEmployerIds,
        List<Long> removedAddressIds) {

    public ContactChanges {
        Objects.requireNonNull(customerNumber, "customerNumber must not be null");
        firstName = trim(firstName);
        familyName = trim(familyName);
        phoneCountryCode = phoneCountryCode == null || phoneCountryCode.isBlank() ? null : phoneCountryCode.trim();
        phoneNumber = trim(phoneNumber);
        email = trim(email);
        removedEmployerIds = removedEmployerIds == null ? List.of() : List.copyOf(removedEmployerIds);
        removedAddressIds = removedAddressIds == null ? List.of() : List.copyOf(removedAddressIds);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
