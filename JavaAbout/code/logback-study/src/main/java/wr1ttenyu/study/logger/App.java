package wr1ttenyu.study.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        HelloLogback helloLogback = new HelloLogback();
        helloLogback.hello();
        helloLogback.testInvokeScope2();
        System.out.println("Hello World!");
    }
}
