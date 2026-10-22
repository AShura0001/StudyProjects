package lavs.business.model;

/**
 * One of a customer's phone numbers, with its country code, whether it can receive texts,
 * and whether it is the primary one.
 */
public class PhoneNumber implements  CustomerType {
    private boolean _canTxt;
    private String _countryCode;
    private String _customerName;
    private String _customerNumber;
    private long _id;
    private boolean _isPrimary;
    private String _number;
    private String _otherType;
    private String _type;

    public PhoneNumber(boolean _canTxt, String _countryCode, String _customerName, String _customerNumber, long _id, boolean _isPrimary, String _otherType, String _type) {
        this._canTxt = _canTxt;
        this._countryCode = _countryCode;
        this._customerName = _customerName;
        this._customerNumber = _customerNumber;
        this._id = _id;
        this._isPrimary = _isPrimary;
        this._otherType = _otherType;
        this._type = _type;
    }

    public boolean is_canTxt() {
        return _canTxt;
    }

    public void set_canTxt(boolean _canTxt) {
        this._canTxt = _canTxt;
    }

    public String get_countryCode() {
        return _countryCode;
    }

    public void set_countryCode(String _countryCode) {
        this._countryCode = _countryCode;
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

    public boolean is_isPrimary() {
        return _isPrimary;
    }

    public void set_isPrimary(boolean _isPrimary) {
        this._isPrimary = _isPrimary;
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

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }
}
