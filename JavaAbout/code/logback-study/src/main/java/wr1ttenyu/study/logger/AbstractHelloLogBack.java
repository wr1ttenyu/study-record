package wr1ttenyu.study.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHelloLogBack {
    private Logger Log = LoggerFactory.getLogger(AbstractHelloLogBack.class);

    protected void testInvokeScope() {
        Log.info("parent testInvokeScope ....");
    }

    protected void testInvokeScope2() {
        Log.info("parent testInvokeScope2 ....");
        testInvokeScope();
    }

}
