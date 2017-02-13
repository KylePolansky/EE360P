
/*
 * EIDS=KPP446,JC82563
 */
import java.util.concurrent.Semaphore; // for implementation using Semaphores

public class CyclicBarrier {
	private Semaphore mutex = new Semaphore(1);
        private Semaphore s1 = new Semaphore(0);
        private Semaphore s2 = new Semaphore(1);
        private int parties;
        private int index;
        private int count;
        
	public CyclicBarrier(int parties) {
            this.parties=parties;
            index=parties-1;
        }
	
	public int await() throws InterruptedException {
            mutex.acquire();
            count++;
            int myIndex = index;
            index--;
            if (count==parties){
                s2.acquire();
                s1.release();
            }
            mutex.release();

            s1.acquire();
            s1.release();

            mutex.acquire();
            count--;
            index++;
            if (count == 0) {
              s1.acquire();
              s2.release();
            }
            mutex.release();
            
            s2.acquire();
            s2.release();
            // you need to write this code
	    return myIndex;
	}
}
