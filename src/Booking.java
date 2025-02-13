public class Booking {
    private int reference;
    private String location;
    private String startDate;
    private String endDate;
    private String customerName;
    private int customerId;

    // Constructor
    public Booking(int reference, String location, String startDate, String endDate, String customerName, int customerId) {
        this.reference = reference;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.customerName = customerName;
        this.customerId = customerId;
    }

    // Getters and Setters
    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "reference=" + reference +
                ", location='" + location + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerId=" + customerId +
                '}';
    }
}
