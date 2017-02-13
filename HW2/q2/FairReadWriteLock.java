
import java.util.concurrent.locks.ReentrantReadWriteLock;

//EIDS=KPP446,JC82563
public class FairReadWriteLock {
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    
    public synchronized void beginRead() throws InterruptedException {
        lock.readLock().lock();
    }

    public synchronized void endRead() {
        lock.readLock().unlock();
    }

    public synchronized void beginWrite() throws InterruptedException{
       lock.writeLock().lock();
    }
    public synchronized void endWrite() {
        lock.writeLock().unlock();
    }
}
	
