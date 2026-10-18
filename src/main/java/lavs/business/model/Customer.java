package lavs.business.model;

public class Customer implements  CustomerType {
    private String dateOfBirth;
    private String familyName;
    private String firstName;
    private String nationality;
    private String number;
    private long organisation;
    private String status;
    private String title;



    public Customer(String dateOfBirth, String familyName, String firstName, String number, String status, String title) {
        this.dateOfBirth = dateOfBirth;
        this.familyName = familyName;
        this.firstName = firstName;
        this.number = number;
        this.status = status;
        this.title = title;
    }

    public Customer() {}

    public String get_dateOfBirth() {
        return dateOfBirth;
    }

    public void set_dateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String get_familyName() {
        return familyName;
    }

    public void set_familyName(String familyName) {
        this.familyName = familyName;
    }

    public String get_firstName() {
        return firstName;
    }

    public void set_firstName(String firstName) {
        this.firstName = firstName;
    }

    public String get_nationality() {
        return nationality;
    }

    public void set_nationality(String nationality) {
        this.nationality = nationality;
    }

    public String get_number() {
        return number;
    }

    public void set_number(String number) {
        this.number = number;
    }

    public String get_status() {
        return status;
    }

    public void set_status(String status) {
        this.status = status;
    }

    public String get_title() {
        return title;
    }

    public long getOrganisation() {
        return organisation;
    }

    public void setOrganisation(long organisation) {
        this.organisation = organisation;
    }

    public void set_title(String title) {
        this.title = title;
    }
}
