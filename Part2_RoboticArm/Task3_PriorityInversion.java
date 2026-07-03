public class Task3_PriorityInversion {

    // timing constants (ms)
    private static final int LOGGER_HOLD_MS   = 1500;
    private static final int SAFETY_DELAY_MS  = 200;
    private static final int PLANNER_DELAY_MS = 400;
    private static final int PLANNER_WORK_MS  = 2000;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Task 3: Priority Inversion Demonstration");
        System.out.println("Scenario:");
        System.out.println("  1. ArmLogger (LOW P=1) acquires lock");
        System.out.println("  2. SafetyMonitor (HIGH P=10) tries to acquire -> BLOCKED");
        System.out.println("  3. MotionPlanner (MED P=5) runs freely, delays logger");
        System.out.println("  4. SafetyMonitor waits longer than expected\n");

        MotorController motor = new MotorController();
        long[] safetyWait = new long[1];

        // Logger acquires lock first and holds it
        Thread loggerThread = new Thread(() -> {
            MotorController.timestamp("ArmLogger [LOW P=1] acquiring lock...");
            synchronized (motor) {
                MotorController.timestamp("ArmLogger [LOW P=1] LOCK ACQUIRED - holding for " + LOGGER_HOLD_MS + " ms");
                long end = System.currentTimeMillis() + LOGGER_HOLD_MS;
                while (System.currentTimeMillis() < end) {
                    Thread.yield();
                }
                MotorController.timestamp("ArmLogger [LOW P=1] LOCK RELEASED");
            }
        }, "ArmLogger");

        // SafetyMonitor tries to acquire after short delay - will block
        Thread safetyThread = new Thread(() -> {
            try { Thread.sleep(SAFETY_DELAY_MS); } catch (InterruptedException e) { return; }
            MotorController.timestamp("SafetyMonitor [HIGH P=10] attempting to acquire lock -> BLOCKED");
            long t0 = System.nanoTime();
            synchronized (motor) {
                long waitMs = (System.nanoTime() - t0) / 1_000_000;
                safetyWait[0] = waitMs;
                MotorController.timestamp("SafetyMonitor [HIGH P=10] LOCK ACQUIRED after " + waitMs + " ms");
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                MotorController.timestamp("SafetyMonitor [HIGH P=10] LOCK RELEASED");
            }
        }, "SafetyMonitor");

        // MotionPlanner runs CPU work - competes with low-priority logger for CPU
        Thread plannerThread = new Thread(() -> {
            try { Thread.sleep(PLANNER_DELAY_MS); } catch (InterruptedException e) { return; }
            MotorController.timestamp("MotionPlanner [MED P=5] running CPU work");
            long end = System.currentTimeMillis() + PLANNER_WORK_MS;
            long counter = 0;
            while (System.currentTimeMillis() < end) { counter++; }
            MotorController.timestamp("MotionPlanner [MED P=5] done (counter=" + counter + ")");
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

        System.out.println("\n--- Priority Inversion Result ---");
        System.out.println("SafetyMonitor (HIGH P=10) waited: " + safetyWait[0] + " ms");
        System.out.println("MotionPlanner (MED P=5) delayed Logger (LOW P=1), blocking SafetyMonitor");
    }
}
