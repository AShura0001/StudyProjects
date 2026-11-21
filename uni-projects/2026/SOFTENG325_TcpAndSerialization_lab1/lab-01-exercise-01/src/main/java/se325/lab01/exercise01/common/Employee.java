package se325.lab01.exercise01.common;

public class Employee {

    private Long id;
    private String name;
    private String jobTitle;
    private int salary;

    public Employee() {

    }

    public Employee(String name, String jobTitle, int salary) {
        this.name = name;
        this.jobTitle = jobTitle;
        this.salary = salary;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public int getSalary() {
        return salary;
    }

    public void setSalary(int salary) {
        this.salary = salary;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" [ID: ");
        sb.append(id);
        sb.append(", Job title: ");
        sb.append(jobTitle);
        sb.append(", Salary: $");
        sb.append(salary);
        sb.append("]");
        return sb.toString();
    }
}
