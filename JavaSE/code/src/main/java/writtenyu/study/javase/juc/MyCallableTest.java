package writtenyu.study.javase.juc;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class MyCallableTest {

    public static void main(String[] args) {

        Callable<String> myCall = new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.currentThread().sleep(4000);
                return "wr1ttenyu";
            }
        };
        FutureTask<String> myFutureTask = new FutureTask(myCall);
        new Thread(myFutureTask).start();
        try {
            String result = myFutureTask.get();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
