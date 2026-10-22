package lavs.ui.component;

import lavs.app.AppServices;
import lavs.business.service.ReferenceDataService;
import lavs.ui.AppContext;
import lavs.ui.LavsApp;
import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.Create;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The lists the forms offer in their drop-downs. Loan values are the
 * mainframe's own spellings, so whatever is picked can be sent as it is.
 *
 * <p>Lists the mainframe keeps as constants, such as address types, come from
 * {@link ReferenceDataService} once they have been fetched after login. Until
 * then, or while offline, the fixed lists below are used instead.
 */
public final class Choices {

    private Choices() {
    }

    /** The mainframe's names for the constant categories the forms use. */
    public static final String ADDRESS_TYPE_CATEGORY = "CustomerAddressType";
    public static final String RATE_TYPE_CATEGORY = "LoanRateType";
    public static final String RATE_FREQUENCY_CATEGORY = "LoanRateFrequency";
    public static final String PAYMENT_FREQUENCY_CATEGORY = "LoanPaymentFrequency";
    public static final String CO_BORROWER_TYPE_CATEGORY = "LoanCoborrowerType";
    public static final String PHONE_TYPE_CATEGORY = "CustomerPhoneNumberType";
    public static final String EMAIL_TYPE_CATEGORY = "CustomerEmailType";

    /** Every category above, for fetching them all after login. */
    public static final List<String> MAINFRAME_CATEGORIES = List.of(ADDRESS_TYPE_CATEGORY, RATE_TYPE_CATEGORY,
            RATE_FREQUENCY_CATEGORY, PAYMENT_FREQUENCY_CATEGORY, CO_BORROWER_TYPE_CATEGORY, PHONE_TYPE_CATEGORY,
            EMAIL_TYPE_CATEGORY);

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

    /** The mainframe's employer types (its customer.addEmployer message spells them this way). */
    public static final List<String> EMPLOYER_TYPES = List.of("Primary", "Secondary");

    /** The mainframe's phone number types. */
    public static final List<String> PHONE_TYPES = List.of("Home", "Mobile", "Work", "Other");

    /** The mainframe's email types. */
    public static final List<String> EMAIL_TYPES = List.of("Personal", "Work", "Other");

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

    public static List<String> addressTypes() {
        return fromMainframe(ADDRESS_TYPE_CATEGORY, ADDRESS_TYPES);
    }

    public static List<String> rateTypes() {
        return fromMainframe(RATE_TYPE_CATEGORY, RATE_TYPES);
    }

    public static List<String> rateFrequencies() {
        return fromMainframe(RATE_FREQUENCY_CATEGORY, RATE_FREQUENCIES);
    }

    public static List<String> paymentFrequencies() {
        return fromMainframe(PAYMENT_FREQUENCY_CATEGORY, PAYMENT_FREQUENCIES);
    }

    public static List<String> phoneTypes() {
        return fromMainframe(PHONE_TYPE_CATEGORY, PHONE_TYPES);
    }

    public static List<String> emailTypes() {
        return fromMainframe(EMAIL_TYPE_CATEGORY, EMAIL_TYPES);
    }

    public static List<String> coBorrowerTypes() {
        return fromMainframe(CO_BORROWER_TYPE_CATEGORY, CO_BORROWER_TYPES);
    }

    /**
     * The category's values from the mainframe if they have already been fetched,
     * otherwise the fixed list. Never waits for the mainframe, so it is safe on the
     * JavaFX thread.
     */
    public static List<String> fromMainframe(String category, List<String> fixed) {
        AppContext context = LavsApp.context();
        return pick(context == null ? Optional.empty() : context.services().flatMap(AppServices::referenceData),
                category, fixed);
    }

    static List<String> pick(Optional<ReferenceDataService> referenceData, String category, List<String> fixed) {
        return referenceData.flatMap(data -> data.cachedValues(category))
                .filter(values -> !values.isEmpty())
                .orElse(fixed);
    }

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
