package lavs.business.model;

public class Employer {
    private String _customerName;
    private String _customerNumber;
    private long _id;
    private String _name;
    private String _number;
    private String _position;
    private double _salary;
    private String _type;

    public Employer(String _customerName, String _customerNumber, long _id, String _name, String _position, double _salary, String _type) {
        this._customerName = _customerName;
        this._customerNumber = _customerNumber;
        this._id = _id;
        this._name = _name;
        this._position = _position;
        this._salary = _salary;
        this._type = _type;
    }

    public Employer() {
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

    public String get_name() {
        return _name;
    }

    public void set_name(String _name) {
        this._name = _name;
    }

    public String get_number() {
        return _number;
    }

    public void set_number(String _number) {
        this._number = _number;
    }

    public String get_position() {
        return _position;
    }

    public void set_position(String _position) {
        this._position = _position;
    }

    public double get_salary() {
        return _salary;
    }

    public void set_salary(double _salary) {
        this._salary = _salary;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }
}
