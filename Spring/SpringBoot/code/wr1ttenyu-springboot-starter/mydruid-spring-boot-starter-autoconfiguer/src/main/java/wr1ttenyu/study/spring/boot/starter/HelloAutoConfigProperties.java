package wr1ttenyu.study.spring.boot.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wr1ttenyu.hello")
public class HelloAutoConfigProperties {

    private String helloPrefix;

    private String helloSuffix;

    public String getHelloPrefix() {
        return helloPrefix;
    }

    public void setHelloPrefix(String helloPrefix) {
        this.helloPrefix = helloPrefix;
    }

    public String getHelloSuffix() {
        return helloSuffix;
    }

    public void setHelloSuffix(String helloSuffix) {
        this.helloSuffix = helloSuffix;
    }
}
