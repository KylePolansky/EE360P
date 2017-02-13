//EIDS=KPP446,JC82563

public class MonitorCyclicBarrier {
    private int parties;
    private int index;

    public MonitorCyclicBarrier(int parties) {
        this.parties = parties;
        index = parties - 1;
    }

    public synchronized int await() throws InterruptedException {
        int myIndex = index;
        index--;

        if(index >= 0){
            this.wait();
        }
        else{
            index=parties - 1;
            notifyAll();
        }

        return myIndex;
    }
}
