package lavs.business.model;

public class Address {
    private String _city;
    private String _country;
    private String _customerName;
    private String _customerNumber;
    private Long _id;
    private boolean _isMailing;
    private boolean _isPrimary;
    private String _line1;
    private String _line2;
    private String _otherType;
    private String _postCode;
    private String _suburb;
    private String _type;

    public Address(){}

    public Address(String _city, String _country, String _customerName, String _customerNumber, Long _id, boolean _isMailing, boolean _isPrimary, String _line1, String _line2, String _otherType, String _postCode, String _suburb, String _type) {
        this._city = _city;
        this._country = _country;
        this._customerName = _customerName;
        this._customerNumber = _customerNumber;
        this._id = _id;
        this._isMailing = _isMailing;
        this._isPrimary = _isPrimary;
        this._line1 = _line1;
        this._line2 = _line2;
        this._otherType = _otherType;
        this._postCode = _postCode;
        this._suburb = _suburb;
        this._type = _type;
    }

    public String get_city() {
        return _city;
    }

    public void set_city(String _city) {
        this._city = _city;
    }

    public String get_country() {
        return _country;
    }

    public void set_country(String _country) {
        this._country = _country;
    }

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }

    public boolean is_isMailing() {
        return _isMailing;
    }

    public void set_isMailing(boolean _isMailing) {
        this._isMailing = _isMailing;
    }

    public boolean is_isPrimary() {
        return _isPrimary;
    }

    public void set_isPrimary(boolean _isPrimary) {
        this._isPrimary = _isPrimary;
    }

    public String get_line1() {
        return _line1;
    }

    public void set_line1(String _line1) {
        this._line1 = _line1;
    }

    public String get_line2() {
        return _line2;
    }

    public void set_line2(String _line2) {
        this._line2 = _line2;
    }

    public String get_otherType() {
        return _otherType;
    }

    public void set_otherType(String _otherType) {
        this._otherType = _otherType;
    }

    public String get_postCode() {
        return _postCode;
    }

    public void set_postCode(String _postCode) {
        this._postCode = _postCode;
    }

    public String get_suburb() {
        return _suburb;
    }

    public void set_suburb(String _suburb) {
        this._suburb = _suburb;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
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
}
