package lavs.ui.component;

import lavs.business.model.Customer;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import lavs.business.service.RuleViolationException;
import lavs.business.service.ValidationError;
import lavs.business.service.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Turns business values into screen text, and business errors into field messages. */
public final class Format {

    private Format() {
    }

    public static String name(Customer customer) {
        String name = (text(customer.get_firstName()) + " " + text(customer.get_familyName())).trim();
        return name.isEmpty() ? "(no name)" : name;
    }

    /** "$1,234.56", or "" when there is no amount. */
    public static String money(BigDecimal amount) {
        return amount == null ? "" : String.format(Locale.US, "$%,.2f", amount);
    }

    /** "1234.56" for an input box, or "" when there is no amount. */
    public static String plain(BigDecimal amount) {
        return amount == null ? "" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** "2025-01-09", or "" when there is no date. */
    public static String date(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /** The mainframe has no loan names, so a loan is called after its number: "LN-5004" reads "Loan 5004". */
    public static String loanName(String loanNumber) {
        int dash = loanNumber.lastIndexOf('-');
        return "Loan " + (dash < 0 ? loanNumber : loanNumber.substring(dash + 1));
    }

    public static String text(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Field name to message for a business error, so forms can mark the right box.
     * {@code fieldNames} maps the business field names to the screen's own keys;
     * anything unmapped goes under "form".
     */
    public static Map<String, String> fieldErrors(Throwable error, Map<String, String> fieldNames) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (error instanceof ValidationException invalid) {
            for (ValidationError e : invalid.getErrors()) {
                errors.putIfAbsent(fieldNames.getOrDefault(e.getField(), "form"),
                        sentence(labelled(e.getField(), e.getMessage())));
            }
        } else if (error instanceof RuleViolationException broken) {
            broken.getProblems().forEach((field, message) ->
                    errors.putIfAbsent(fieldNames.getOrDefault(field, "form"), sentence(message)));
        } else if (error instanceof RecordRejectedException refused) {
            refused.getFieldErrors().forEach((field, message) ->
                    errors.putIfAbsent(fieldNames.getOrDefault(field, "form"), sentence(message)));
            if (errors.isEmpty()) {
                errors.put("form", sentence(refused.getMessage() == null || refused.getMessage().isBlank()
                        ? "The mainframe did not accept the change" : refused.getMessage()));
            }
        }
        return errors;
    }

    /** True for errors that {@link #fieldErrors} can describe. */
    public static boolean isRuleError(Throwable error) {
        return error instanceof ValidationException || error instanceof RuleViolationException
                || error instanceof RecordRejectedException;
    }

    /** A message for something that went wrong talking to the mainframe. */
    public static String problem(Throwable error, String fallback) {
        if (error instanceof MainframeUnavailableException && error.getMessage() != null) {
            return error.getMessage();
        }
        return fallback;
    }

    /**
     * Validators word some messages without the field ("Is required"); this names it:
     * field "postCode" with "Is required" reads "Post code is required".
     */
    static String labelled(String field, String message) {
        if (field == null || field.isBlank() || !(message.startsWith("Is ") || message.startsWith("Must "))) {
            return message;
        }
        String words = field.replaceAll("([a-z])([A-Z0-9])", "$1 $2").toLowerCase(Locale.ROOT);
        String label = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return label + " " + Character.toLowerCase(message.charAt(0)) + message.substring(1);
    }

    private static String sentence(String message) {
        String m = message.trim();
        return m.endsWith(".") ? m : m + ".";
    }
}
