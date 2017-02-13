import com.sun.org.apache.bcel.internal.generic.NEW;
import org.junit.*;

public class testGarden {

    @Test
    public void OnlyFillHoles() throws InterruptedException {
        Garden garden = new Garden();
        //should stop at 4;
        Thread t = new Thread(new Newton(garden, 1000));
        t.start();
        Thread.sleep(100);
        Assert.assertTrue(garden.totalHolesDugByNewton() == 4);
    }

    @Test
    public void DoAllThree() throws InterruptedException {
        Garden garden = new Garden();
        Thread t = new Thread(new Newton(garden,1000));
        Thread t1 = new Thread(new Benjamin(garden, 1000));
        Thread t2 = new Thread(new Mary(garden, 1000));
        t.start();
        Thread.sleep(333);
        t1.start();
        Thread.sleep(333);
        t2.start();
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1000);
            printStats(garden);
        }
    }

    private void printStats(Garden garden) {
        System.out.println("Dug: " + garden.totalHolesDugByNewton() + " ; Seeded: " + garden.totalHolesSeededByBenjamin() + " ; Filled: " + garden.totalHolesFilledByMary());
    }
    //Digs hole
    private class Newton implements Runnable {

        int ms;
        Garden garden;

        public Newton(Garden garden, int ms) {
            this.garden = garden;
            this.ms = ms;
        }

        @Override
        public void run() {
            while (true) {
                System.out.println("Starting to Dig. Thread: " + Thread.currentThread().getId());
                try {
                    garden.startDigging();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Digging for " + ms + "ms. Thread: " + Thread.currentThread().getId());
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                garden.doneDigging();
                System.out.println("Done Digging. Thread: " + Thread.currentThread().getId());
            }
        }
    }

    //Places seed in hole
    private class Benjamin implements Runnable {

        int ms;
        Garden garden;
        public Benjamin(Garden garden, int ms) {
            this.garden =garden;
            this.ms = ms;
        }

        @Override
        public void run() {
            while (true) {
                System.out.println("Starting to Seed. Thread: " + Thread.currentThread().getId());
                try {
                    garden.startSeeding();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Seeding for " + ms + "ms. Thread: " + Thread.currentThread().getId());
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                garden.doneSeeding();
                System.out.println("Done Seeding. Thread: " + Thread.currentThread().getId());
            }
        }
    }

    //Fills hold
    private class Mary implements Runnable {

        int ms;
        Garden garden;
        public Mary(Garden garden, int ms) {
            this.garden = garden;
            this.ms = ms;
        }
        @Override
        public void run() {
            while (true) {
                System.out.println("Starting to Fill. Thread: " + Thread.currentThread().getId());
                try {
                    garden.startDigging();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Filling for " + ms + "ms. Thread: " + Thread.currentThread().getId());
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                garden.doneFilling();
                System.out.println("Done Filling. Thread: " + Thread.currentThread().getId());
            }
        }
    }
}
