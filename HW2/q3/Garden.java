
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//EIDS=KPP446,JC82563 
public class Garden {
    int dugHoles;
    int seededHoles;
    int filledHoles;

    boolean shovelBussy = false;

    ReentrantLock lock = new ReentrantLock();
    Condition noHoles = lock.newCondition(); //Benjamin cannot plant a seed unless at least one empty hole exists. 
    Condition noSeeds = lock.newCondition(); //Mary cannot fill a hole unless at least one hole exists in which Benjamin has planted a seed. 
    Condition notSeededHoles = lock.newCondition(); //Newton has to wait for Benjamin if there are 4 holes dug which have not been seeded yet. 
    Condition notFilledHoles = lock.newCondition(); //Newton has to wait for Mary if there are 8 unlled holes. 
    Condition shovel = lock.newCondition(); //There is only one shovel that can be used to dig and ll holes. 

    public Garden() {
    }

    public void startDigging() throws InterruptedException {
        lock.lock();
        try {
            System.out.println("Holes Dug: " + totalHolesDugByNewton() + " ; Holes Seeded: " + totalHolesSeededByBenjamin());
            while ((totalHolesDugByNewton() - totalHolesSeededByBenjamin()) >= 4)
                notSeededHoles.await();
            while ((totalHolesDugByNewton() - totalHolesFilledByMary()) >= 8)
                notFilledHoles.await();
            while (shovelBussy)
                shovel.await();
            shovelBussy = true;
        } finally {
            lock.unlock();
        }
    }

    public void doneDigging() {
        lock.lock();
        try {
            shovelBussy = false;
            shovel.signal();
            dugHoles++;
            noHoles.signal();
        } finally {
            lock.unlock();
        }
    }

    public void startSeeding() throws InterruptedException {
        lock.lock();
        try {
            while ((totalHolesDugByNewton() - totalHolesSeededByBenjamin()) == 0)
                noHoles.await();
        } finally {
            lock.unlock();
        }
    }

    public void doneSeeding() {
        lock.lock();
        try {
            seededHoles++;
            noSeeds.signal();
            notSeededHoles.signal();
        } finally {
            lock.unlock();
        }
    }

    public void startFilling() throws InterruptedException {
        lock.lock();
        try {
            while ((totalHolesSeededByBenjamin() - totalHolesFilledByMary()) == 0)
                noHoles.await();
            while (shovelBussy)
                shovel.await();
            shovelBussy = true;
        } finally {
            lock.unlock();
        }
    }

    public void doneFilling() {
        lock.lock();
        try {
            shovelBussy = false;
            shovel.signal();
            filledHoles++;
            notFilledHoles.signal();
        } finally {
            lock.unlock();
        }
    }

    /* 
    * The following methods return the total number of holes dug, seeded or  
    * filled by Newton, Benjamin or Mary at the time the methods' are  
    * invoked on the garden class. */
    public int totalHolesDugByNewton() {
        return dugHoles;
    }

    public int totalHolesSeededByBenjamin() {
        return seededHoles;
    }

    public int totalHolesFilledByMary() {
        return filledHoles;
    }
}