package writtenyu.study.javase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import writtenyu.study.javase.bean.SubUser;
import writtenyu.study.javase.bean.User;
import writtenyu.study.javase.threadPoolMonitor.ExecutorsUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wr1ttenyu
 */
/*@SpringBootApplication*/
public class DemoApplication implements Runnable {

    private static int k = 0;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorsUtil.class);

    private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    public static void main(String[] args) throws IOException {
        /*ExecutorsUtil executorsUtil = new ExecutorsUtil(5, 6,
                1000, TimeUnit.SECONDS, new ArrayBlockingQueue(100), "testsync");

        for (int i = 0; i < 10; i++) {
            executorsUtil.execute(new DemoApplication());
        }

        try {
            Thread.currentThread().sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        /*AtomicInteger atomicInteger = new AtomicInteger(1);
        atomicInteger.getAndAdd(1);

        Timer timer = new Timer();

        Random random = new Random();
        random.nextInt();

        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(5);*/

        TreeMap<String, String> stringStringTreeMap = new TreeMap<>();
        ConcurrentHashMap<String,String> stringStringConcurrentHashMap = new ConcurrentHashMap<>();
        stringStringConcurrentHashMap.put(null, null);


        SubUser subUser = new SubUser();
        subUser.setT(new Date());
    }

    static class Singleton {
        private User helper = null;

        public User getHelper() {
            System.out.println(Singleton.this);
            if (helper == null) {
                synchronized (this) {
                    if (helper == null)
                        helper = new User();
                }
            }
            return helper;
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
