package lavs.business.model;

/**
 * A document stored against a customer on the mainframe: its file name, file type and
 * size, and what kind of document it is.
 */
public class Document implements  CustomerType {
    private String customerName;
    private String customerNumber;
    private String fileName;
    private String fileType;
    private long id;
    private String number;
    private long size;
    private String type;

    public Document(String customerName, String customerNumber, String fileName, String fileType, long id, long size, String type) {
        this.customerName = customerName;
        this.customerNumber = customerNumber;
        this.fileName = fileName;
        this.fileType = fileType;
        this.id = id;
        this.size = size;
        this.type = type;
    }

    public Document() {
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
