package wr1ttenyu.study.facets.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingMain {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(LoggingMain.class);
        logger.info("Hello World");
    }

}
