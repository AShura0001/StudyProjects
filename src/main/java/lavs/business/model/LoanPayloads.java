package lavs.business.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lavs.business.model.CustomerReference.DraftCustomer;
import lavs.business.model.CustomerReference.ExistingCustomer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * The text saved inside the drafts of a loan and of a co-borrower.
 *
 * <p>The code that creates these drafts writes the text with this class, and the
 * code that sends them to the mainframe reads it with this class, so the format is
 * written down in one place. Both are JSON objects. Money is kept as text so
 * nothing is rounded, and dates are written as {@code yyyy-MM-dd}.
 *
 * <p>A loan draft looks like this:
 *
 * <pre>
 * {"record":"loan",
 *  "customer":{"kind":"existing","number":"01-0001-0000020"},
 *  "principal":"250000.00","rateType":"Fixed","rateAmount":"5.85",
 *  "rateFrequency":"Yearly","paymentAmount":"1450.00","paymentFrequency":"Monthly",
 *  "startDate":"2026-10-01","renewalDate":"2031-10-01"}
 * </pre>
 *
 * <p>A co-borrower draft looks like this:
 *
 * <pre>
 * {"record":"coBorrower","type":"GUARANTOR",
 *  "customer":{"kind":"draft","id":"6f1c..."}}
 * </pre>
 *
 * <p>The {@code customer} is the loan's owner for a loan and the co-borrower for a
 * co-borrower. When it is {@code existing}, the number is already known. When it is
 * {@code draft}, the draft depends on that customer's draft and the mainframe's
 * number is handed over when it is sent. In the draft's list of dependencies the
 * loan comes first for a co-borrower, then the co-borrower's own customer draft if
 * there is one.
 */
public final class LoanPayloads {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RECORD = "record";
    private static final String LOAN_RECORD = "loan";
    private static final String CO_BORROWER_RECORD = "coBorrower";
    private static final String CUSTOMER = "customer";
    private static final String KIND = "kind";
    private static final String EXISTING = "existing";
    private static final String DRAFT = "draft";

    /**
     * What a loan draft holds.
     *
     * @param customer the customer the loan belongs to
     * @param details the values of the loan
     */
    public record LoanContent(CustomerReference customer, LoanDetails details) {

        /**
         * Creates the content.
         *
         * @throws NullPointerException if either value is null
         */
        public LoanContent {
            Objects.requireNonNull(customer, "customer must not be null");
            Objects.requireNonNull(details, "details must not be null");
        }
    }

    /**
     * What a co-borrower draft holds.
     *
     * @param type the part the customer plays on the loan
     * @param customer the customer who is the co-borrower or guarantor
     */
    public record CoBorrowerContent(CoBorrowerType type, CustomerReference customer) {

        /**
         * Creates the content.
         *
         * @throws NullPointerException if either value is null
         */
        public CoBorrowerContent {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(customer, "customer must not be null");
        }
    }

    private LoanPayloads() {
    }

    /**
     * Writes the text for a loan draft.
     *
     * @param customer the customer the loan belongs to
     * @param details the values of the loan
     * @return the text to save in the draft
     * @throws NullPointerException if either argument is null
     */
    public static String loanPayload(CustomerReference customer, LoanDetails details) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(details, "details must not be null");
        ObjectNode node = MAPPER.createObjectNode();
        node.put(RECORD, LOAN_RECORD);
        putCustomer(node, customer);
        node.put("principal", details.principal().toPlainString());
        node.put("rateType", details.rateType());
        node.put("rateAmount", details.rateAmount().toPlainString());
        node.put("rateFrequency", details.rateFrequency());
        node.put("paymentAmount", details.paymentAmount().toPlainString());
        node.put("paymentFrequency", details.paymentFrequency());
        node.put("startDate", details.startDate().toString());
        node.put("renewalDate", details.renewalDate().toString());
        return node.toString();
    }

    /**
     * Reads the text of a loan draft.
     *
     * @param payload the text saved in the draft
     * @return what the draft holds
     * @throws NullPointerException if the payload is null
     * @throws IllegalArgumentException if the text is not a valid loan draft
     */
    public static LoanContent readLoan(String payload) {
        JsonNode node = parse(payload, LOAN_RECORD);
        CustomerReference customer = readCustomer(node);
        LoanDetails details = new LoanDetails(
                decimal(node, "principal"),
                text(node, "rateType"),
                decimal(node, "rateAmount"),
                text(node, "rateFrequency"),
                decimal(node, "paymentAmount"),
                text(node, "paymentFrequency"),
                date(node, "startDate"),
                date(node, "renewalDate"));
        return new LoanContent(customer, details);
    }

    /**
     * Writes the text for a co-borrower draft.
     *
     * @param type the part the customer plays on the loan
     * @param customer the customer who is the co-borrower or guarantor
     * @return the text to save in the draft
     * @throws NullPointerException if either argument is null
     */
    public static String coBorrowerPayload(CoBorrowerType type, CustomerReference customer) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(customer, "customer must not be null");
        ObjectNode node = MAPPER.createObjectNode();
        node.put(RECORD, CO_BORROWER_RECORD);
        node.put("type", type.name());
        putCustomer(node, customer);
        return node.toString();
    }

    /**
     * Reads the text of a co-borrower draft.
     *
     * @param payload the text saved in the draft
     * @return what the draft holds
     * @throws NullPointerException if the payload is null
     * @throws IllegalArgumentException if the text is not a valid co-borrower draft
     */
    public static CoBorrowerContent readCoBorrower(String payload) {
        JsonNode node = parse(payload, CO_BORROWER_RECORD);
        return new CoBorrowerContent(CoBorrowerType.valueOf(text(node, "type")), readCustomer(node));
    }

    private static void putCustomer(ObjectNode node, CustomerReference customer) {
        ObjectNode reference = node.putObject(CUSTOMER);
        if (customer instanceof ExistingCustomer existing) {
            reference.put(KIND, EXISTING);
            reference.put("number", existing.customerNumber());
        } else {
            reference.put(KIND, DRAFT);
            reference.put("id", ((DraftCustomer) customer).draftId());
        }
    }

    private static CustomerReference readCustomer(JsonNode node) {
        JsonNode reference = node.get(CUSTOMER);
        if (reference == null || !reference.isObject()) {
            throw new IllegalArgumentException("the payload has no customer");
        }
        String kind = text(reference, KIND);
        return switch (kind) {
            case EXISTING -> CustomerReference.existing(text(reference, "number"));
            case DRAFT -> CustomerReference.draft(text(reference, "id"));
            default -> throw new IllegalArgumentException("the customer kind is not known: " + kind);
        };
    }

    private static JsonNode parse(String payload, String expectedRecord) {
        Objects.requireNonNull(payload, "payload must not be null");
        JsonNode node;
        try {
            node = MAPPER.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("the payload is not valid JSON", e);
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("the payload is not a JSON object");
        }
        if (!expectedRecord.equals(text(node, RECORD))) {
            throw new IllegalArgumentException("the payload is not a " + expectedRecord + " payload");
        }
        return node;
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("the payload is missing the text value " + name);
        }
        return value.asText();
    }

    private static BigDecimal decimal(JsonNode node, String name) {
        String value = text(node, name);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("the payload value " + name + " is not a number: " + value, e);
        }
    }

    private static LocalDate date(JsonNode node, String name) {
        String value = text(node, name);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("the payload value " + name + " is not a date: " + value, e);
        }
    }
}
