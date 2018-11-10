package wr1ttenyu.study.netty.timeserver.bean;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.util.List;

public class Customer {
    @XStreamAsAttribute
    private Long customerNumber;
    private String firstName;
    private String lastName;
    private List<String> middleName;

    public Long getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(Long customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<String> getMiddleName() {
        return middleName;
    }

    public void setMiddleName(List<String> middleName) {
        this.middleName = middleName;
    }
}
