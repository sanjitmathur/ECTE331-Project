import java.util.concurrent.locks.ReentrantLock;

// Simulates Priority Inheritance Protocol - Java does not support this natively
// When a high-priority thread is blocked, the lock holder inherits its priority
class PriorityInheritanceLock {

    private final ReentrantLock lock = new ReentrantLock();
    private volatile Thread owner = null;
    private volatile int ownerOriginalPriority = Thread.NORM_PRIORITY;

    public void lock() {
        Thread caller = Thread.currentThread();

        // if a lower-priority thread holds the lock, boost its priority
        if (lock.isLocked() && owner != null && owner != caller) {
            if (owner.getPriority() < caller.getPriority()) {
                MotorController.timestamp("INHERITANCE | " + owner.getName()
                        + " priority boosted: " + owner.getPriority() + " -> " + caller.getPriority());
                owner.setPriority(caller.getPriority());
            }
        }

        lock.lock();
        owner = caller;
        ownerOriginalPriority = caller.getPriority();
    }

    public void unlock() {
        Thread releasing = Thread.currentThread();
        int currentPriority = releasing.getPriority();
        owner = null;
        lock.unlock();

        // restore original priority if it was boosted
        if (currentPriority != ownerOriginalPriority) {
            releasing.setPriority(ownerOriginalPriority);
            MotorController.timestamp("INHERITANCE | " + releasing.getName()
                    + " priority restored: " + currentPriority + " -> " + ownerOriginalPriority);
        }
    }

    public boolean isLocked() {
        return lock.isLocked();
    }
}

public class Task4_PriorityInheritance {

    private static final int LOGGER_HOLD_MS   = 1500;
    private static final int SAFETY_DELAY_MS  = 200;
    private static final int PLANNER_DELAY_MS = 400;
    private static final int PLANNER_WORK_MS  = 2000;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Task 4: Priority Inheritance Protocol");
        System.out.println("When SafetyMonitor (HIGH) blocks on ArmLogger (LOW),");
        System.out.println("ArmLogger temporarily inherits HIGH priority.\n");

        PriorityInheritanceLock piLock = new PriorityInheritanceLock();
        MotorController motor = new MotorController();
        long[] safetyWait = new long[1];

        Thread loggerThread = new Thread(() -> {
            MotorController.timestamp("ArmLogger [LOW P=1] acquiring PI lock...");
            piLock.lock();
            try {
                MotorController.timestamp("ArmLogger [LOW P=1] PI LOCK ACQUIRED");
                motor.accessInheritance("ArmLogger", LOGGER_HOLD_MS);
            } finally {
                piLock.unlock();
                MotorController.timestamp("ArmLogger [LOW P=1] PI LOCK RELEASED");
            }
        }, "ArmLogger");

        Thread safetyThread = new Thread(() -> {
            try { Thread.sleep(SAFETY_DELAY_MS); } catch (InterruptedException e) { return; }
            MotorController.timestamp("SafetyMonitor [HIGH P=10] attempting PI lock -> triggers inheritance");
            long t0 = System.nanoTime();
            piLock.lock();
            try {
                long waitMs = (System.nanoTime() - t0) / 1_000_000;
                safetyWait[0] = waitMs;
                MotorController.timestamp("SafetyMonitor [HIGH P=10] PI LOCK ACQUIRED after " + waitMs + " ms");
                motor.accessInheritance("SafetyMonitor", 300);
            } finally {
                piLock.unlock();
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

        System.out.println("\n--- Priority Inheritance Result ---");
        System.out.println("SafetyMonitor (HIGH P=10) waited: " + safetyWait[0] + " ms");
        System.out.println("Compare with Task 3 result to see improvement.");
    }
}
