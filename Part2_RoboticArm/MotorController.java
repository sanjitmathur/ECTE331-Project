import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MotorController {

    public static final int CEILING_PRIORITY = Thread.MAX_PRIORITY;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Task 1 - no synchronisation
    public void accessUnsynchronised(String caller, int durationMs) {
        doWork(caller, durationMs);
    }

    // Task 2 - synchronized mutual exclusion
    public synchronized void accessSynchronised(String caller, int durationMs) {
        doWork(caller, durationMs);
    }

    // Task 4 - called by PriorityInheritanceLock
    public void accessInheritance(String caller, int durationMs) {
        doWork(caller, durationMs);
    }

    // Task 5 - priority ceiling protocol
    public synchronized void accessCeiling(String caller, int durationMs) {
        Thread t = Thread.currentThread();
        int saved = t.getPriority();
        t.setPriority(CEILING_PRIORITY);
        timestamp("CEILING | " + caller + " boosted to " + CEILING_PRIORITY);
        try {
            doWork(caller, durationMs);
        } finally {
            t.setPriority(saved);
            timestamp("CEILING | " + caller + " restored to " + saved);
        }
    }

    private void doWork(String caller, int durationMs) {
        timestamp("ENTER | " + caller + " acquired MotorController");
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        timestamp("EXIT  | " + caller + " released MotorController");
    }

    static void timestamp(String message) {
        System.out.println("[" + LocalTime.now().format(TS) + "] " + message);
    }
}
