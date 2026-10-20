package lavs.business.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The text saved inside a CUSTOMER draft.
 *
 * <p>Mirrors {@link LoanPayloads}: the code that creates a customer draft writes
 * the text with this class, and the code that sends it to the mainframe reads it
 * with this class. There is no separate DraftType for an address, phone number,
 * email or employer, so a new customer's initial contact details are bundled
 * inside the one customer payload, the same way a loan's fields are bundled
 * inside one loan payload rather than split into several drafts.
 *
 * <p>A customer draft looks like this:
 *
 * <pre>
 * {"record":"customer",
 *  "title":"Mr","firstName":"Jane","familyName":"Doe","dateOfBirth":"1990-01-01",
 *  "nationality":"NZ","organisation":"1",
 *  "addresses":[{"type":"Home","line1":"1 Queen Street","line2":"","suburb":"",
 *                "city":"Auckland","postCode":"1010","country":"New Zealand",
 *                "otherType":"","isMailing":true,"isPrimary":true}],
 *  "phoneNumbers":[{"type":"Mobile","countryCode":"64","number":"211234567",
 *                    "otherType":"","isPrimary":true,"canTxt":true}],
 *  "emails":[{"type":"Personal","address":"jane@example.com","otherType":"","isPrimary":false}],
 *  "employers":[{"type":"Full-time","name":"Acme Ltd","position":"Developer","salary":"75000.0"}]}
 * </pre>
 *
 * <p>Each list may be empty. Values the mainframe assigns (customer number,
 * status, and each child record's own id/number) are never part of the payload,
 * since none of them exist yet for a draft.
 */
public final class CustomerPayloads {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RECORD = "record";
    private static final String CUSTOMER_RECORD = "customer";

    /**
     * What a customer draft holds: the customer's own fields plus whatever
     * address/phone/email/employer records were entered for them before the
     * draft was saved.
     */
    public record CustomerContent(Customer customer, List<Address> addresses, List<PhoneNumber> phoneNumbers, List<Email> emails, List<Employer> employers) {

        public CustomerContent {
            Objects.requireNonNull(customer, "customer must not be null");
            addresses = List.copyOf(Objects.requireNonNull(addresses, "addresses must not be null"));
            phoneNumbers = List.copyOf(Objects.requireNonNull(phoneNumbers, "phoneNumbers must not be null"));
            emails = List.copyOf(Objects.requireNonNull(emails, "emails must not be null"));
            employers = List.copyOf(Objects.requireNonNull(employers, "employers must not be null"));
        }
    }

    private CustomerPayloads() {
    }

    /**
     * Writes the text for a customer draft.
     *
     * @throws NullPointerException if any argument is null
     */
    public static String customerPayload(Customer customer, List<Address> addresses, List<PhoneNumber> phoneNumbers, List<Email> emails, List<Employer> employers) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(addresses, "addresses must not be null");
        Objects.requireNonNull(phoneNumbers, "phoneNumbers must not be null");
        Objects.requireNonNull(emails, "emails must not be null");
        Objects.requireNonNull(employers, "employers must not be null");

        ObjectNode node = MAPPER.createObjectNode();
        node.put(RECORD, CUSTOMER_RECORD);
        node.put("title", customer.get_title());
        node.put("firstName", customer.get_firstName());
        node.put("familyName", customer.get_familyName());
        node.put("dateOfBirth", customer.get_dateOfBirth());
        node.put("nationality", customer.get_nationality());
        node.put("organisation", Long.toString(customer.getOrganisation()));

        ArrayNode addressArray = node.putArray("addresses");
        for (Address a : addresses) {
            ObjectNode n = addressArray.addObject();
            n.put("type", a.getType());
            n.put("line1", a.getLine1());
            n.put("line2", nullToEmpty(a.getLine2()));
            n.put("suburb", nullToEmpty(a.getSuburb()));
            n.put("city", nullToEmpty(a.getCity()));
            n.put("postCode", nullToEmpty(a.getPostCode()));
            n.put("country", a.getCountry());
            n.put("otherType", nullToEmpty(a.getOtherType()));
            n.put("isMailing", Boolean.TRUE.equals(a.getIsMailing()));
            n.put("isPrimary", Boolean.TRUE.equals(a.getIsPrimary()));
        }

        ArrayNode phoneArray = node.putArray("phoneNumbers");
        for (PhoneNumber p : phoneNumbers) {
            ObjectNode n = phoneArray.addObject();
            n.put("type", p.get_type());
            n.put("countryCode", p.get_countryCode());
            n.put("number", p.get_number());
            n.put("otherType", nullToEmpty(p.get_otherType()));
            n.put("isPrimary", p.is_isPrimary());
            n.put("canTxt", p.is_canTxt());
        }

        ArrayNode emailArray = node.putArray("emails");
        for (Email e : emails) {
            ObjectNode n = emailArray.addObject();
            n.put("type", e.getType());
            n.put("address", e.getAddress());
            n.put("otherType", nullToEmpty(e.getOtherType()));
            n.put("isPrimary", e.isPrimary());
        }

        ArrayNode employerArray = node.putArray("employers");
        for (Employer emp : employers) {
            ObjectNode n = employerArray.addObject();
            n.put("type", emp.get_type());
            n.put("name", emp.get_name());
            n.put("position", nullToEmpty(emp.get_position()));
            n.put("salary", Double.toString(emp.get_salary()));
        }

        return node.toString();
    }

    /**
     * Reads the text of a customer draft.
     *
     * @throws NullPointerException if the payload is null
     * @throws IllegalArgumentException if the text is not a valid customer draft
     */
    public static CustomerContent readCustomer(String payload) {
        JsonNode node = parse(payload);

        Customer customer = new Customer();
        customer.set_title(optionalText(node, "title"));
        customer.set_firstName(text(node, "firstName"));
        customer.set_familyName(optionalText(node, "familyName"));
        customer.set_dateOfBirth(optionalText(node, "dateOfBirth"));
        customer.set_nationality(optionalText(node, "nationality"));
        customer.setOrganisation(longValue(node, "organisation"));

        List<Address> addresses = new ArrayList<>();
        for (JsonNode n : arrayOf(node, "addresses")) {
            Address a = new Address();
            a.setType(text(n, "type"));
            a.setLine1(text(n, "line1"));
            a.setLine2(optionalText(n, "line2"));
            a.setSuburb(optionalText(n, "suburb"));
            a.setCity(optionalText(n, "city"));
            a.setPostCode(optionalText(n, "postCode"));
            a.setCountry(text(n, "country"));
            a.setOtherType(optionalText(n, "otherType"));
            a.setIsMailing(boolValue(n, "isMailing"));
            a.setIsPrimary(boolValue(n, "isPrimary"));
            addresses.add(a);
        }

        List<PhoneNumber> phoneNumbers = new ArrayList<>();
        for (JsonNode n : arrayOf(node, "phoneNumbers")) {
            PhoneNumber p = new PhoneNumber(boolValue(n, "canTxt"), text(n, "countryCode"), null, null,
                0L, boolValue(n, "isPrimary"), optionalText(n, "otherType"), text(n, "type"));
            p.set_number(text(n, "number"));
            phoneNumbers.add(p);
        }

        List<Email> emails = new ArrayList<>();
        for (JsonNode n : arrayOf(node, "emails")) {
            emails.add(new Email(text(n, "address"), null, null, 0L, boolValue(n, "isPrimary"),
                optionalText(n, "otherType"), text(n, "type")));
        }

        List<Employer> employers = new ArrayList<>();
        for (JsonNode n : arrayOf(node, "employers")) {
            employers.add(new Employer(null, null, 0L, text(n, "name"), optionalText(n, "position"),
                doubleValue(n, "salary"), text(n, "type")));
        }

        return new CustomerContent(customer, addresses, phoneNumbers, emails, employers);
    }

    private static JsonNode parse(String payload) {
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
        if (!CUSTOMER_RECORD.equals(optionalText(node, RECORD))) {
            throw new IllegalArgumentException("the payload is not a customer payload");
        }
        return node;
    }

    private static Iterable<JsonNode> arrayOf(JsonNode node, String name) {
        JsonNode array = node.get(name);
        if (array == null || !array.isArray()) {
            return List.of();
        }
        return array;
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("the payload is missing the text value " + name);
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return (value == null || !value.isTextual()) ? null : value.asText();
    }

    private static boolean boolValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value != null && value.asBoolean(false);
    }

    private static long longValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.asText());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("the payload value " + name + " is not a number", e);
        }
    }

    private static double doubleValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("the payload value " + name + " is not a number", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
