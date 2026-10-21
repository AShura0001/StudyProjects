package lavs.ui.component;

import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.Create;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The fixed lists the forms offer in their drop-downs. Loan values are the
 * mainframe's own spellings, so whatever is picked can be sent as it is.
 * Swap these for ReferenceDataService lists once that is connected.
 */
public final class Choices {

    private Choices() {
    }

    public static final List<String> TITLES = List.of("Mr", "Mrs", "Ms", "Miss", "Mx", "Dr");

    public static final List<String> RATE_TYPES = List.of(
            Create.RATETYPE_FIXED, Create.RATETYPE_FLOATING, Create.RATETYPE_INTERESTONLY);

    public static final List<String> RATE_FREQUENCIES = List.of(
            Create.RATEFREQUENCY_YEARLY, Create.RATEFREQUENCY_MONTHLY, Create.RATEFREQUENCY_FORTNIGHTLY,
            Create.RATEFREQUENCY_WEEKLY, Create.RATEFREQUENCY_DAILY);

    public static final List<String> PAYMENT_FREQUENCIES = List.of(
            Create.PAYMENTFREQUENCY_WEEKLY, Create.PAYMENTFREQUENCY_FORTNIGHTLY, Create.PAYMENTFREQUENCY_MONTHLY,
            Create.PAYMENTFREQUENCY_YEARLY, Create.PAYMENTFREQUENCY_ONRENEWAL);

    /** The mainframe's address types (its customer.addAddress message spells them this way). */
    public static final List<String> ADDRESS_TYPES = List.of("Residential", "Postal", "Forwarding", "Delegated", "Other");

    public static final List<String> CO_BORROWER_TYPES = List.of(
            AddCoborrower.TYPE_COBORROWER, AddCoborrower.TYPE_GUARANTOR);

    /** Every country's English name, sorted, for the nationality list. */
    public static final List<String> COUNTRIES = Stream.of(Locale.getISOCountries())
            .map(code -> new Locale("", code).getDisplayCountry(Locale.ENGLISH))
            .filter(name -> !name.isBlank())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();

    /** Dialling codes, closest to home first. */
    public static final List<String> COUNTRY_CODES = List.of(
            "+64", "+61", "+679", "+685", "+676", "+682", "+1", "+44", "+353", "+86", "+852", "+886",
            "+91", "+63", "+81", "+82", "+65", "+60", "+66", "+62", "+84", "+27", "+49", "+33");

    private static final Map<String, String> CODE_PLACES = Map.ofEntries(
            Map.entry("+64", "New Zealand"), Map.entry("+61", "Australia"), Map.entry("+679", "Fiji"),
            Map.entry("+685", "Samoa"), Map.entry("+676", "Tonga"), Map.entry("+682", "Cook Islands"),
            Map.entry("+1", "USA / Canada"), Map.entry("+44", "United Kingdom"), Map.entry("+353", "Ireland"),
            Map.entry("+86", "China"), Map.entry("+852", "Hong Kong"), Map.entry("+886", "Taiwan"),
            Map.entry("+91", "India"), Map.entry("+63", "Philippines"), Map.entry("+81", "Japan"),
            Map.entry("+82", "South Korea"), Map.entry("+65", "Singapore"), Map.entry("+60", "Malaysia"),
            Map.entry("+66", "Thailand"), Map.entry("+62", "Indonesia"), Map.entry("+84", "Vietnam"),
            Map.entry("+27", "South Africa"), Map.entry("+49", "Germany"), Map.entry("+33", "France"));

    private static final Map<String, String> LABELS = Map.of(
            Create.RATETYPE_INTERESTONLY, "Interest only",
            Create.PAYMENTFREQUENCY_ONRENEWAL, "On renewal",
            AddCoborrower.TYPE_COBORROWER, "Co-borrower");

    /** How a value is shown to the user, e.g. "InterestOnly" reads "Interest only". */
    public static String label(String value) {
        return value == null ? "" : LABELS.getOrDefault(value, value);
    }

    /** "+64" reads "+64  New Zealand" in the drop-down list. */
    public static String codeLabel(String code) {
        String place = CODE_PLACES.get(code);
        return place == null ? code : code + "  " + place;
    }
}
