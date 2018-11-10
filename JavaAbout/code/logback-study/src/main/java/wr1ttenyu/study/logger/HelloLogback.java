package wr1ttenyu.study.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloLogback extends AbstractHelloLogBack{

    private Logger Log = LoggerFactory.getLogger(App.class);

    public String hello() {
        String hello = "hello logback";
        Log.info(hello);
        return hello;
    }

    @Override
    public void testInvokeScope() {
        Log.info("sub testInvokeScope ....");
    }

    @Override
    public void testInvokeScope2() {
        Log.info("sub testInvokeScope2 ....");
        super.testInvokeScope2();
    }
}
