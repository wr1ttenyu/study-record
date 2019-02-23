package writtenyu.study.javase.juc;

import java.util.concurrent.CountDownLatch;

public class MyCountDownLatchTest {

    public static void main(String[] args) {
        CountDownLatch countDownLatch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            System.out.println(countDownLatch.getCount());
            new Thread(() -> {
                try {
                    Thread.currentThread().sleep(5000);
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("123");
    }

}
