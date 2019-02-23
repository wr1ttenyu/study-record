package writtenyu.study.javase.juc;

public class ProducerAndConsumer2 {

    public static void main(String[] args) {
        SyncTikcet2 syncTikcet = new SyncTikcet2(1);

        Runnable syncSoldTicket = () -> syncTikcet.soldTicket();
        Runnable syncRefundTicket = () -> syncTikcet.refund();

        new Thread(syncSoldTicket, "soldWorker").start();
        new Thread(syncRefundTicket, "refundWorker").start();
    }
}

class SyncTikcet2 {

    private int ticketNum;

    public SyncTikcet2(Integer ticketNum) {
        this.ticketNum = ticketNum;
    }


    public void soldTicket() {
        while (true) {
            if (ticketNum > 0) {
                ticketNum = ticketNum - 1;
                System.out.println("sold ticket,the num is" + ticketNum);
            } else {
            }
        }
    }

    public void refund() {
        while (true) {
            if (ticketNum == 0) {
                ticketNum = ticketNum + 1;
                System.out.println("refund ticket,the num is" + ticketNum);
            } else  {
            }
        }
    }

    public Integer getTicketNum() {
        return ticketNum;
    }

    public void setTicketNum(Integer ticketNum) {
        this.ticketNum = ticketNum;
    }
}