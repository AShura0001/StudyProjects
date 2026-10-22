package lavs.business.model;

/**
 * One of a customer's email addresses. One email can be the primary one.
 */
public class Email implements  CustomerType {
    private String address;
    private String customerName;
    private String customerNumber;
    private long id;
    private boolean isPrimary;
    private String number;
    private String otherType;
    private String type;

    public Email(String address, String customerName, String customerNumber, long id, boolean isPrimary, String otherType, String type) {
        this.address = address;
        this.customerName = customerName;
        this.customerNumber = customerNumber;
        this.id = id;
        this.isPrimary = isPrimary;
        this.otherType = otherType;
        this.type = type;
    }

    public Email() {
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getOtherType() {
        return otherType;
    }

    public void setOtherType(String otherType) {
        this.otherType = otherType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
