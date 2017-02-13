import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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

        public boolean isWrite() {
            return isWrite;
        }
        
    }
    
    int timestamp=0;
    Map<Long,Timestamp> timeMap = new TreeMap<Long,Timestamp>();
    
    public synchronized void beginRead() throws InterruptedException {
        acquireTimestamp(false);
        boolean myTurn=false;
        while(!myTurn){
           
            int myTimestamp = getTimestamp().getTimestamp();
            myTurn=true;
            for(Entry<Long,Timestamp> e : timeMap.entrySet())
            {
                Timestamp t = e.getValue();
                if(t.getTimestamp()<myTimestamp && t.isWrite()){
                    myTurn=false;
                    break;
                }
            }
            if(!myTurn)
                wait();
        }
    }

    public synchronized void endRead() {
        removeTimestamp();
        notifyAll();
    }

    public synchronized void beginWrite() throws InterruptedException{
        acquireTimestamp(true);
        boolean myTurn=false;
        while(!myTurn){
            int myTimestamp = getTimestamp().getTimestamp();
            myTurn=true;
            for(Entry<Long,Timestamp> e : timeMap.entrySet())
            {
                Timestamp t = e.getValue();
                if(t.getTimestamp()<myTimestamp){
                    myTurn=false;
                    break;
                }
            }
            if(!myTurn)
                wait();
        }
    }
    public synchronized void endWrite() {
        removeTimestamp();
        notifyAll();
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
	
