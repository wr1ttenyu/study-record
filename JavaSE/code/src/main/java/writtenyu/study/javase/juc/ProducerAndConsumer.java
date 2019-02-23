package writtenyu.study.javase.juc;

import writtenyu.study.javase.threadPoolMonitor.ExecutorsUtil;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerAndConsumer {

    public static void main(String[] args) {
        SyncTikcet syncTikcet = new SyncTikcet(3);
        SyncTikcet syncTikcet2 = new SyncTikcet(3);
        LockTicket lockTicket = new LockTicket(3);
        ExecutorsUtil executorsUtil = new ExecutorsUtil(2, 2, 1, TimeUnit.SECONDS, new SynchronousQueue(),
                "ticketworks");

        Runnable syncSoldTicket = () -> syncTikcet.soldTicket();
        Runnable syncRefundTicket = () -> syncTikcet.refund();
        Runnable lockSoldTicket = () -> lockTicket.soldTicket();
        Runnable lockRefundTicket = () -> lockTicket.refund();

        new Thread(syncSoldTicket, "soldWorker").start();
        new Thread(syncRefundTicket, "refundWorker").start();
        /*executorsUtil.execute(syncSoldTicket);
        executorsUtil.execute(syncRefundTicket);*/
        /*executorsUtil.execute(lockSoldTicket);
        executorsUtil.execute(lockRefundTicket);*/

    }
}

class SyncTikcet {

    private Integer ticketNum;

    public SyncTikcet(Integer ticketNum) {
        this.ticketNum = ticketNum;
    }

    /*public void testAwait() {
        this.wait();
    }*/

    public void soldTicket() {
        while (true) {
            System.out.println("1去往蚌埠的票还剩" + ticketNum + "张");
            /*synchronized (this) {*/
                if (ticketNum > 0) {
                    ticketNum -= 1;
                    System.out.println("去往蚌埠的票卖了一张，还剩" + ticketNum + "张");
                    /*this.notify();*/
                } else {
                    /*System.out.println("1去往蚌埠的票还剩" + ticketNum + "张");*/
                    /*try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }
            /*}*/
        }
    }

    public void refund() {
        while (true) {
            System.out.println("2去往蚌埠的票还剩" + ticketNum + "张");
            /*synchronized (this) {*/
                if (ticketNum == 0) {
                    ticketNum += 1;
                    System.out.println("去往蚌埠的票退了一张，还剩" + ticketNum + "张");
                    /*this.notify();*/
                } else {
                   /*System.out.println("2去往蚌埠的票还剩" + ticketNum + "张");*/
                    /*try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }
           /* }*/
        }
    }

    public Integer getTicketNum() {
        return ticketNum;
    }

    public void setTicketNum(Integer ticketNum) {
        this.ticketNum = ticketNum;
    }
}

class LockTicket {

    private Integer ticketNum;

    private Lock lock;

    private Condition condition;

    public LockTicket(Integer ticketNum) {
        this.ticketNum = ticketNum;
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    public void soldTicket() {
        while (true) {
            try {
                lock.lock();
                if (ticketNum > 0) {
                    ticketNum -= 1;
                    System.out.println("去往蚌埠的票卖了一张，还剩" + ticketNum + "张");
                    condition.signal();
                } else {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void refund() {
        while (true) {
            try {
                lock.lock();
                if (ticketNum == 0) {
                    ticketNum += 1;
                    System.out.println("去往蚌埠的票退了一张，还剩" + ticketNum + "张");
                    condition.signal();
                } else {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}