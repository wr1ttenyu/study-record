package writtenyu.study.javase.juc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MyCyclicBarrierTest {

    public static void main(String[] args) {
        /*CyclicBarrier cyclicBarrier = new CyclicBarrier(8, () -> {
            System.out.println("我是全职猎人！");
        });*/
        CyclicBarrier cyclicBarrier = new CyclicBarrier(9, () -> {
            System.out.println("我是全职猎人！");
        });

        for (int i = 0; i < 8; i++) {
            int num = i;
            new Thread(() ->{
                System.out.println("第" + num + "关闯关成功！");
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }).start();
        }


        try {
            cyclicBarrier.await();
            System.out.println("123");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
