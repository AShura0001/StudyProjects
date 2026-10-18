package lavs.business.model;

public class Address  implements  CustomerType {
    private String city;
    private String country;
    private String customerName;
    private String customerNumber;
    private long id;
    private Boolean isMailing;
    private Boolean isPrimary;
    private String line1;
    private String line2;
    private String number;
    private String otherType;
    private String postCode;
    private String suburb;
    private String type;

    public Address(String city, String country, String customerName, String customerNumber, Long id, boolean isMailing, boolean isPrimary, String line1, String line2, String number, String otherType, String postCode, String suburb, String type) {
        this.city = city;
        this.country = country;
        this.customerName = customerName;
        this.customerNumber = customerNumber;
        this.id = id;
        this.isMailing = isMailing;
        this.isPrimary = isPrimary;
        this.line1 = line1;
        this.line2 = line2;
        this.number = number;
        this.otherType = otherType;
        this.postCode = postCode;
        this.suburb = suburb;
        this.type = type;
    }

    public Address() {}

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getIsMailing() {
        return isMailing;
    }

    public void setIsMailing(Boolean isMailing) {
        this.isMailing = isMailing;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
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

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getSuburb() {
        return suburb;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
