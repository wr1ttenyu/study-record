package wr1ttenyu.study.javase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoApplicationTests {

    @Test
    public void contextLoads() {

        byte[] bytesbuffer4 = new byte[] { 11 };
        byte[] bytesEachCache = new byte[] { 123 };
        System.arraycopy(bytesbuffer4, 0, bytesEachCache, bytesEachCache.length, bytesbuffer4.length);
        System.out.println(bytesEachCache);
    }

}
