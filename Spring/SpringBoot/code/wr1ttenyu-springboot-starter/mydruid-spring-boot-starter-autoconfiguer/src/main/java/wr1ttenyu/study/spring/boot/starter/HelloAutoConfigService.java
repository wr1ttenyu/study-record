package wr1ttenyu.study.spring.boot.starter;

public class HelloAutoConfigService {

    private HelloAutoConfigProperties helloAutoConfigProperties;

    public String sayHello(String name) {
        return helloAutoConfigProperties.getHelloPrefix() + "-" + name + helloAutoConfigProperties.getHelloSuffix();
    }

    public HelloAutoConfigProperties getHelloAutoConfigProperties() {
        return helloAutoConfigProperties;
    }

    public void setHelloAutoConfigProperties(HelloAutoConfigProperties helloAutoConfigProperties) {
        this.helloAutoConfigProperties = helloAutoConfigProperties;
    }
}
