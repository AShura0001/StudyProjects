package lavs.business.model;

/**
 * A note on a customer: its title (the type), its text, and who added it and when.
 */
public class Note implements  CustomerType {
    private String _author;
    private String _customerName;
    private String _customerNumber;
    private String _dateAdded;
    private long _id;
    private String _lines;
    private String _number;
    private String _numberOfLines;
    private String _type;

    public Note(String _author, String _customerName, String _customerNumber, String _dateAdded, long _id, String _lines, String _numberOfLines, String _type) {
        this._author = _author;
        this._customerName = _customerName;
        this._customerNumber = _customerNumber;
        this._dateAdded = _dateAdded;
        this._id = _id;
        this._lines = _lines;
        this._numberOfLines = _numberOfLines;
        this._type = _type;
    }

    public Note() {}

    public String get_author() {
        return _author;
    }

    public void set_author(String _author) {
        this._author = _author;
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

    public String get_dateAdded() {
        return _dateAdded;
    }

    public void set_dateAdded(String _dateAdded) {
        this._dateAdded = _dateAdded;
    }

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }

    public String get_lines() {
        return _lines;
    }

    public void set_lines(String _lines) {
        this._lines = _lines;
    }

    public String get_number() {
        return _number;
    }

    public void set_number(String _number) {
        this._number = _number;
    }

    public String get_numberOfLines() {
        return _numberOfLines;
    }

    public void set_numberOfLines(String _numberOfLines) {
        this._numberOfLines = _numberOfLines;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }
}
