package wr1ttenyu.study.netty.timeserver.protocol.http.xml;

import wr1ttenyu.study.netty.timeserver.bean.Address;
import wr1ttenyu.study.netty.timeserver.bean.Customer;
import wr1ttenyu.study.netty.timeserver.bean.Order;
import wr1ttenyu.study.netty.timeserver.bean.Shipping;

public class OrderFactory {

    public static Order create(long orderID) {
        Order order = new Order();
        order.setOrderNumber(orderID);
        order.setTotal(9999.999f);
        Address address = new Address();
        address.setCity("南京市");
        address.setCountry("中国");
        address.setPostCode("123321");
        address.setState("江苏省");
        address.setStreetl("龙眠大道");
        order.setBillTo(address);
        Customer customer = new Customer();
        customer.setCustomerNumber(orderID);
        customer.setFirstName("李");
        customer.setLastName("林峰");
        order.setCustomer(customer);
        order.setShipping(Shipping.INTERNATIONAL_EXPRESS);
        order.setShipTo(address);
        return order;
    }
}
