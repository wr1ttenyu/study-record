package writtenyu.study.javase;

import writtenyu.study.javase.bean.User;
import writtenyu.study.javase.threadPoolMonitor.ExecutorsUtil;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author wr1ttenyu
 */
/*@SpringBootApplication*/
public class DemoApplication implements Runnable {

    private static int k = 0;

    public static void main(String[] args) {
        ExecutorsUtil executorsUtil = new ExecutorsUtil(5, 6,
                1000, TimeUnit.SECONDS, new ArrayBlockingQueue(100), "testsync");

        for (int i = 0; i < 10; i++) {
            executorsUtil.execute(new DemoApplication());
        }

        try {
            Thread.currentThread().sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Random random = new Random();
        int randomInt = random.nextInt(3);
        testsync(randomInt);
    }

    private void testsync(int j) {
        ArrayList<User> test = new ArrayList<>();
        User user = null;
        for (int i = 0; i < 3; i++) {
            user = new User();
            user.setName(String.valueOf(i));
            test.add(user);
        }

        user = test.get(j);

        synchronized (user) {
            String userName = "user:" + user.getName();
            System.out.println(userName + " 进入线程");
            k++;
            System.out.println(userName + " k值为:" + k);
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
