public class Plant implements Runnable {
    // How long do we want to run the juice processing
    public static final long PROCESSING_TIME = 5 * 1000;

    private static final int NUM_PLANTS = 2;

    public static void main(String[] args) {
        // Startup the plants

        Plant[] plants = new Plant[NUM_PLANTS];
        for (int i = 0; i < NUM_PLANTS; i++) {
            plants[i] = new Plant(0);
            plants[i].startPlant();
        }

        // Give the plants time to do work
        delay(PROCESSING_TIME, "Plant malfunction");

        // Stop the plant, and wait for it to shutdown
        for (Plant p : plants) {
            p.stopPlant();
        }

        for (Plant p : plants) {
            p.waitToStop();
        }

        // Summarize the results
        int totalProvided = 0;
        int totalProcessed = 0;
        int totalBottles = 0;
        int totalWasted = 0;
        for (Plant p : plants) {
            totalProvided += p.getProvidedOranges();
            totalProcessed += p.getProcessedOranges();
            totalBottles += p.getBottles();
            totalWasted += p.getWaste();
        }
        System.out.println("Total provided/processed = " + totalProvided + "/" + totalProcessed);
        System.out.println("Created " + totalBottles + ", wasted " + totalWasted + " oranges");
    }

    private static void delay(long time, String errMsg) {
        long sleepTime = Math.max(1, time);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            System.err.println(errMsg);
        }
    }

    public final int ORANGES_PER_BOTTLE = 3;

    // plant class variables
    private final Thread worker1;
    private final Thread worker2;
    private int orangesProvided;
    private int orangesProcessed;
    private volatile boolean timeToWork;

    /*
     * create one for each step this will allow us to proccess at each step and sync
     * shared resources and notify when the resource isn't used.
     */
    private ReaderBlockList fetchedOranges = new ReaderBlockList();
    private ReaderBlockList peeledOranges = new ReaderBlockList();
    private ReaderBlockList juicedOranges = new ReaderBlockList();
    private ReaderBlockList bottledOranges = new ReaderBlockList();

    // this is my plant constructer with two threads
    Plant(int plantNum) {
        orangesProvided = 0;
        orangesProcessed = 0;
        worker1 = new Thread(this, "Worker 1[" + plantNum + "]");
        worker2 = new Thread(this, "Worker 2[" + plantNum + "]");
    }

    // starts the plants and workers
    public void startPlant() {
        timeToWork = true;
        worker1.start();
        worker2.start();
    }

    // stops when time is over
    public void stopPlant() {
        timeToWork = false;
    }

    // stops the threads in each plant
    public void waitToStop() {
        try {
            worker1.join();
            worker2.join();

        } catch (InterruptedException e) {
            System.err.println(worker1.getName() + " stop malfunction or " + worker1.getName() + " stop malfunction");
        }
    }

    /*
     * this run method implements my threads, each plant has two workers that can
     * either deal with peeling and juicing or bottling and processing
     */
    public void run() {
        while (timeToWork) {
            if (fetchedOranges.isEmpty()) {
                fetchedOranges.add(new Orange());
                System.out.print(".");
                orangesProvided++;
            }
            // Work for first thread
            if ("Worker 1[0]".equals(Thread.currentThread().getName())) {
                if (!fetchedOranges.isEmpty()) {
                    orangePeel(fetchedOranges.get());
                }
                if (!peeledOranges.isEmpty()) {
                    orangeJuice(peeledOranges.get());
                }
            }
            // Work for second thread on plant
            if ("Worker 2[0]".equals(Thread.currentThread().getName()) && !juicedOranges.isEmpty()) {
                if (!juicedOranges.isEmpty()) {
                    orangeBottle(juicedOranges.get());
                }
                if (!bottledOranges.isEmpty()) {
                    orangeProcess(bottledOranges.get());
                }
            }
        }
        System.out.println("");
    }

    // method for Peeling the orange, runs process till orange isn't Fetched state
    public void orangePeel(Orange o) {
        while (o.getState() == Orange.State.Fetched) {
            o.runProcess();
            peeledOranges.add(o);

        }
    }

    // method for juicing the orange, runs process till orange isn't Peeled state
    public void orangeJuice(Orange o) {
        while (o.getState() == Orange.State.Peeled) {
            o.runProcess();
            juicedOranges.add(o);
        }
    }

    // method for bottling the orange, runs process till orange isn't Squeezed state
    private void orangeBottle(Orange o) {
        while (o.getState() == Orange.State.Squeezed) {
            o.runProcess();
            bottledOranges.add(o);
        }
    }

    // method finishes processing the orange and throws it away as its scraps
    private void orangeProcess(Orange o) {
        while (o.getState() == Orange.State.Bottled) {
            o.runProcess();
        }
        orangesProcessed++;
    }

    // returns the provided oranges
    private int getProvidedOranges() {
        return orangesProvided;
    }

    // getter method for processed oranges
    public int getProcessedOranges() {
        return orangesProcessed;
    }

    // returns the total bottles of oranges juice
    public int getBottles() {
        return orangesProcessed / ORANGES_PER_BOTTLE;
    }

    // returns the total oranges that didn't finish processing
    public int getWaste() {
        return orangesProvided - orangesProcessed;
    }
}
