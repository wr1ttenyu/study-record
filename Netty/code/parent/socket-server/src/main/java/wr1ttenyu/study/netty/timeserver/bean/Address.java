package wr1ttenyu.study.netty.timeserver.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("billTo")
public class Address {

    private String streetl;
    private String street2;
    private String city;
    private String state;
    private String postCode;
    private String country;

    public String getStreetl() {
        return streetl;
    }

    public void setStreetl(String streetl) {
        this.streetl = streetl;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
