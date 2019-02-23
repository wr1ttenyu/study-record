package writtenyu.study.javase.juc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多线程之间按顺序调用，实现 A -> B -> C
 * 三个线程启动，要求如下：
 * AA 打印5次 BB 打印10次 CC打印15次
 * 轮询10轮
 */
public class ThreadOrderAccess {

    public static void main(String[] args) {
        Integer curFlag = 1;
        PrintWorker printWorkerA = new PrintWorker(PrintWorkerName.PrintA);
        PrintWorker printWorkerB = new PrintWorker(PrintWorkerName.PrintB);
        PrintWorker printWorkerC = new PrintWorker(PrintWorkerName.PrintC);

        new Thread(printWorkerB, "workerB").start();
        new Thread(printWorkerC, "workerC").start();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(printWorkerA, "workerA").start();
    }
}

class PrintWorker implements Runnable {

    private static Lock lock = new ReentrantLock();
    private static Condition condition1 = lock.newCondition();
    private static Condition condition2 = lock.newCondition();
    private static Condition condition3 = lock.newCondition();
    private static Integer curFlag = 1;

    private int flag;
    private String printStr;
    private Integer printCount;
    private Condition curCondition;
    private Condition nextWorkerCondition;

    public PrintWorker(PrintWorkerName workerName) {
        /*this.curFlag = curFlag;*/

        switch (workerName) {
            case PrintA:
                curCondition = condition1;
                nextWorkerCondition = condition2;
                printStr = "AA";
                printCount = 1;
                flag = 1;
                break;
            case PrintB:
                curCondition = condition2;
                nextWorkerCondition = condition3;
                printStr = "BB";
                printCount = 2;
                flag = 2;
                break;
            case PrintC:
                curCondition = condition3;
                nextWorkerCondition = condition1;
                printStr = "CC";
                printCount = 3;
                flag = 3;
                break;
            default:
                throw new RuntimeException("Worker Name Not Exist!");
        }
    }

    private void printStr() {
        lock.lock();
        try {
            // 判断
            while (curFlag != flag) {
                curCondition.await();
            }

            // 干活
            for (int i = 0; i < printCount; i++) {
                System.out.println(Thread.currentThread().getName() + " print:" + printStr + " num:" + i);
            }

            //通知
            curFlag += 1;
            if (curFlag > 3) {
                curFlag = 1;
            }
            nextWorkerCondition.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            printStr();
        }
    }
}

enum PrintWorkerName {
    PrintA(1), PrintB(2), PrintC(3);

    private final int name;

    PrintWorkerName(int name) {
        this.name = name;
    }

    public int getName() {
        return name;
    }
}