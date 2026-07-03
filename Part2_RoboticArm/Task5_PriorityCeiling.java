import java.util.concurrent.locks.ReentrantLock;

// Priority Ceiling Protocol - any thread acquiring the lock runs at ceiling priority
class PriorityCeilingLock {

    private final int ceilingPriority;
    private final ReentrantLock lock = new ReentrantLock();
    private int holderOriginalPriority;

    public PriorityCeilingLock(int ceilingPriority) {
        this.ceilingPriority = ceilingPriority;
    }

    public void lock() {
        lock.lock();
        Thread current = Thread.currentThread();
        holderOriginalPriority = current.getPriority();
        current.setPriority(ceilingPriority);
        MotorController.timestamp("CEILING | " + current.getName()
                + " priority: " + holderOriginalPriority + " -> " + ceilingPriority);
    }

    public void unlock() {
        Thread current = Thread.currentThread();
        current.setPriority(holderOriginalPriority);
        lock.unlock();
        MotorController.timestamp("CEILING | " + current.getName()
                + " priority restored to " + holderOriginalPriority);
    }
}

public class Task5_PriorityCeiling {

    private static final int CEILING = Thread.MAX_PRIORITY;
    private static final int LOGGER_HOLD_MS   = 1500;
    private static final int SAFETY_DELAY_MS  = 200;
    private static final int PLANNER_DELAY_MS = 400;
    private static final int PLANNER_WORK_MS  = 2000;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Task 5: Priority Ceiling Protocol");
        System.out.println("Ceiling priority = " + CEILING);
        System.out.println("Any thread holding the lock runs at ceiling priority.\n");

        PriorityCeilingLock pcLock = new PriorityCeilingLock(CEILING);
        MotorController motor = new MotorController();
        long[] safetyWait = new long[1];

        Thread loggerThread = new Thread(() -> {
            MotorController.timestamp("ArmLogger [LOW P=1] acquiring ceiling lock...");
            pcLock.lock();
            try {
                MotorController.timestamp("ArmLogger [P=" + Thread.currentThread().getPriority() + "] CEILING LOCK ACQUIRED");
                motor.accessInheritance("ArmLogger", LOGGER_HOLD_MS);
            } finally {
                pcLock.unlock();
            }
        }, "ArmLogger");

        Thread safetyThread = new Thread(() -> {
            try { Thread.sleep(SAFETY_DELAY_MS); } catch (InterruptedException e) { return; }
            MotorController.timestamp("SafetyMonitor [HIGH P=10] attempting ceiling lock...");
            long t0 = System.nanoTime();
            pcLock.lock();
            try {
                long waitMs = (System.nanoTime() - t0) / 1_000_000;
                safetyWait[0] = waitMs;
                MotorController.timestamp("SafetyMonitor [P=" + Thread.currentThread().getPriority()
                        + "] CEILING LOCK ACQUIRED after " + waitMs + " ms");
                motor.accessInheritance("SafetyMonitor", 300);
            } finally {
                pcLock.unlock();
            }
        }, "SafetyMonitor");

        Thread plannerThread = new Thread(() -> {
            try { Thread.sleep(PLANNER_DELAY_MS); } catch (InterruptedException e) { return; }
            MotorController.timestamp("MotionPlanner [MED P=5] running CPU work");
            long end = System.currentTimeMillis() + PLANNER_WORK_MS;
            long counter = 0;
            while (System.currentTimeMillis() < end) { counter++; }
            MotorController.timestamp("MotionPlanner [MED P=5] done");
        }, "MotionPlanner");

        loggerThread.setPriority(ArmLogger.PRIORITY);
        safetyThread.setPriority(SafetyMonitor.PRIORITY);
        plannerThread.setPriority(MotionPlanner.PRIORITY);

        loggerThread.start();
        Thread.sleep(50);
        safetyThread.start();
        plannerThread.start();

        loggerThread.join();
        safetyThread.join();
        plannerThread.join();

        System.out.println("\n--- Priority Ceiling Result ---");
        System.out.println("SafetyMonitor (HIGH P=10) waited: " + safetyWait[0] + " ms");
        System.out.println("Logger ran at ceiling priority - not preempted by MotionPlanner.");
    }
}
