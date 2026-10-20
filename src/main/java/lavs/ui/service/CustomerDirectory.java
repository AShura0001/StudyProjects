package lavs.ui.service;

import java.util.List;
import java.util.Map;

/**
 * What the customer screens need from the layer below. Every method blocks,
 * so callers must keep them off the JavaFX thread. Failures to reach the
 * mainframe surface as unchecked exceptions.
 */
public interface CustomerDirectory {

    record Match(String name, String customerNumber) {
    }

    record Employer(String name, String id) {
    }

    record Loan(String name, String date, String loanNumber) {
    }

    record Details(String customerNumber, String firstName, String lastName, String phone,
                   String email, String addressLine1, String addressLine2, String notes,
                   List<Employer> employers, List<Loan> loans) {
    }

    /** The fields of the Register customer form; everything arrives trimmed. */
    record NewCustomer(String title, String firstName, String lastName, String dateOfBirth,
                       String nationality) {
    }

    /** Either the new customer number, or field name to message. */
    record Registration(String customerNumber, Map<String, String> errors) {
    }

    /** @return every customer matching the name or number; empty when none */
    List<Match> search(String term);

    int count();

    /** @param page zero-based page index */
    List<Match> page(int page, int size);

    /** @return the customer, or null when the number is unknown */
    Details details(String customerNumber);

    /**
     * @return field name to message; an empty map means the save was accepted
     * @throws NotSupportedYetException when saving is not connected yet
     */
    Map<String, String> save(Details details);

    Registration register(NewCustomer customer);

    /** One employer record; salary is plain digits such as 85000.00. */
    record EmployerDetails(String id, String name, String position, String salary) {
    }

    /** @return the employer, or null when unknown */
    EmployerDetails employer(String customerNumber, String employerId);

    /**
     * @return field name to message; an empty map means the change was accepted
     * @throws NotSupportedYetException when saving is not connected yet
     */
    Map<String, String> saveEmployer(String customerNumber, EmployerDetails employer);

    /** @return the customer's photo as image bytes, or null when there is none */
    byte[] photo(String customerNumber);

    /** @throws NotSupportedYetException when saving is not connected yet */
    void savePhoto(String customerNumber, byte[] image);
}
