//EIDS=KPP446,JC82563

import java.util.concurrent.; // for implementation using Semaphores

public class MonitorCyclicBarrier {
    private int parties;
    private int index;
    private int count;

    public MonitorCyclicBarrier(int parties) {
        this.parties = parties;
        index = parties - 1;
    }

    public int await() throws InterruptedException {
        int myIndex = index;


        return myIndex;
    }
}
