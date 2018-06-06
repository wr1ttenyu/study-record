package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.Test;

class MyTest {

    @Test
    public void myTest() {
        byte[] bytesbuffer4 = new byte[] {1};
        byte[] bytesEachCache = new byte[20];
        System.arraycopy(bytesbuffer4, 0, bytesEachCache, 0, bytesbuffer4.length);
        System.out.println(bytesEachCache);
    }
 
}
