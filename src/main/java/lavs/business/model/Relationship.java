package lavs.business.model;

public class Relationship {
    private String _customerName;
    private String _customerNumber;
    private long _id;
    private String _number;
    private String _otherType;
    private String _secondaryCustomerName;
    private String _secondaryCustomerNumber;
    private String _type;

    public Relationship(String _customerName, String _customerNumber, long _id, String _otherType, String _secondaryCustomerName, String _secondaryCustomerNumber, String _type) {
        this._customerName = _customerName;
        this._customerNumber = _customerNumber;
        this._id = _id;
        this._otherType = _otherType;
        this._secondaryCustomerName = _secondaryCustomerName;
        this._secondaryCustomerNumber = _secondaryCustomerNumber;
        this._type = _type;
    }

    public Relationship() {
    }

    public String get_customerName() {
        return _customerName;
    }

    public void set_customerName(String _customerName) {
        this._customerName = _customerName;
    }

    public String get_customerNumber() {
        return _customerNumber;
    }

    public void set_customerNumber(String _customerNumber) {
        this._customerNumber = _customerNumber;
    }

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }

    public String get_number() {
        return _number;
    }

    public void set_number(String _number) {
        this._number = _number;
    }

    public String get_otherType() {
        return _otherType;
    }

    public void set_otherType(String _otherType) {
        this._otherType = _otherType;
    }

    public String get_secondaryCustomerName() {
        return _secondaryCustomerName;
    }

    public void set_secondaryCustomerName(String _secondaryCustomerName) {
        this._secondaryCustomerName = _secondaryCustomerName;
    }

    public String get_secondaryCustomerNumber() {
        return _secondaryCustomerNumber;
    }

    public void set_secondaryCustomerNumber(String _secondaryCustomerNumber) {
        this._secondaryCustomerNumber = _secondaryCustomerNumber;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }
}
