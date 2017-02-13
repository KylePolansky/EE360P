
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//EIDS=KPP446,JC82563
public class FairReadWriteLock {
    private class Timestamp{
        int timestamp;
        boolean isWrite;

        public Timestamp(int timestamp, boolean isWrite) {
            this.timestamp = timestamp;
            this.isWrite = isWrite;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isWrite() {
            return isWrite;
        }
        
    }
    
    int timestamp=0;
    TreeMap<Long,Timestamp> timeMap = new TreeMap();
    ReentrantLock lock = new ReentrantLock();
    Condition readerIn = lock.newCondition();
    Condition writerIn = lock.newCondition();
    
    public synchronized void beginRead() throws InterruptedException {
        lock.lock();
        acquireTimestamp(false);
        boolean myTurn=false;
        while(!myTurn){
            writerIn.await();
            int myTimestamp = getTimestamp().getTimestamp();
            myTurn=true;
            for(Entry<Long,Timestamp> e : timeMap.entrySet())
            {
                Timestamp t = e.getValue();
                if(t.getTimestamp()<myTimestamp && t.isWrite())
                    myTurn=false;
            }
        }
    }

    public synchronized void endRead() {
        removeTimestamp();
        lock.unlock();
    }

    public synchronized void beginWrite() throws InterruptedException{
        lock.lock();
        acquireTimestamp(true);
        boolean myTurn=false;
        while(!myTurn){
            writerIn.await();
            readerIn.await();
            int myTimestamp = getTimestamp().getTimestamp();
            myTurn=true;
            for(Entry<Long,Timestamp> e : timeMap.entrySet())
            {
                Timestamp t = e.getValue();
                if(t.getTimestamp()<myTimestamp)
                    myTurn=false;
            }
        }
    }
    public synchronized void endWrite() {
        removeTimestamp();
        lock.unlock();
    }
    
    private synchronized void acquireTimestamp(boolean isWrite)
    {
        timestamp++;
        timeMap.put(Thread.currentThread().getId(),new Timestamp(timestamp,isWrite));
    }
    
    private synchronized void removeTimestamp()
    {
        timeMap.remove(Thread.currentThread().getId());
    }
    
    private synchronized Timestamp getTimestamp()
    {
        return timeMap.get(Thread.currentThread().getId());
    }
}
	
