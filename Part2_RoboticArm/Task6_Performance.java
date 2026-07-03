import java.util.concurrent.locks.ReentrantLock;

public class Task6_Performance {

    private static final int RUNS = 5;
    private static final int LOGGER_HOLD_MS   = 1200;
    private static final int SAFETY_DELAY_MS  = 150;
    private static final int PLANNER_DELAY_MS = 300;
    private static final int PLANNER_WORK_MS  = 1600;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Task 6: Performance Evaluation");
        System.out.println("Comparing: Baseline vs Priority Inheritance vs Priority Ceiling");
        System.out.println("Runs per scenario: " + RUNS + "\n");

        long[] baselineResults    = new long[RUNS];
        long[] inheritanceResults = new long[RUNS];
        long[] ceilingResults     = new long[RUNS];

        for (int r = 0; r < RUNS; r++) {
            System.out.println("--- Run " + (r + 1) + "/" + RUNS + " ---");
            baselineResults[r]    = runBaseline();
            inheritanceResults[r] = runInheritance();
            ceilingResults[r]     = runCeiling();
            Thread.sleep(200);
        }

        double avgBaseline    = average(baselineResults);
        double avgInheritance = average(inheritanceResults);
        double avgCeiling     = average(ceilingResults);

        System.out.println("\n--- Results Table ---");
        System.out.printf("%-25s | Average (ms) | Runs%n", "Scenario");
        System.out.println("-".repeat(60));
        System.out.printf("%-25s | %12.1f | %s%n", "1. Baseline (No Mgmt)", avgBaseline, formatRuns(baselineResults));
        System.out.printf("%-25s | %12.1f | %s%n", "2. Priority Inheritance", avgInheritance, formatRuns(inheritanceResults));
        System.out.printf("%-25s | %12.1f | %s%n", "3. Priority Ceiling", avgCeiling, formatRuns(ceilingResults));
        System.out.println();

        double inheritanceSaving = 100.0 * (avgBaseline - avgInheritance) / avgBaseline;
        double ceilingSaving     = 100.0 * (avgBaseline - avgCeiling) / avgBaseline;
        System.out.printf("Priority Inheritance improvement: %.1f%%%n", inheritanceSaving);
        System.out.printf("Priority Ceiling improvement:     %.1f%%%n%n", ceilingSaving);

        // ASCII bar chart
        printChart(avgBaseline, avgInheritance, avgCeiling);
    }

    private static long runBaseline() throws InterruptedException {
        long[] result = new long[1];
        MotorController motor = new MotorController();

        Thread logger  = new Thread(() -> { synchronized (motor) { busySpin(LOGGER_HOLD_MS); } }, "ArmLogger");
        Thread safety  = new Thread(() -> {
            sleepMs(SAFETY_DELAY_MS);
            long t0 = System.nanoTime();
            synchronized (motor) { result[0] = (System.nanoTime() - t0) / 1_000_000; }
        }, "SafetyMonitor");
        Thread planner = new Thread(() -> { sleepMs(PLANNER_DELAY_MS); busySpin(PLANNER_WORK_MS); }, "MotionPlanner");

        logger.setPriority(ArmLogger.PRIORITY);
        safety.setPriority(SafetyMonitor.PRIORITY);
        planner.setPriority(MotionPlanner.PRIORITY);

        logger.start(); Thread.sleep(30); safety.start(); planner.start();
        logger.join(); safety.join(); planner.join();

        System.out.println("  Baseline    -> SafetyMonitor waited " + result[0] + " ms");
        return result[0];
    }

    private static long runInheritance() throws InterruptedException {
        long[] result = new long[1];
        PriorityInheritanceLock piLock = new PriorityInheritanceLock();

        Thread logger  = new Thread(() -> { piLock.lock(); try { busySpin(LOGGER_HOLD_MS); } finally { piLock.unlock(); } }, "ArmLogger");
        Thread safety  = new Thread(() -> {
            sleepMs(SAFETY_DELAY_MS);
            long t0 = System.nanoTime();
            piLock.lock(); try { result[0] = (System.nanoTime() - t0) / 1_000_000; } finally { piLock.unlock(); }
        }, "SafetyMonitor");
        Thread planner = new Thread(() -> { sleepMs(PLANNER_DELAY_MS); busySpin(PLANNER_WORK_MS); }, "MotionPlanner");

        logger.setPriority(ArmLogger.PRIORITY);
        safety.setPriority(SafetyMonitor.PRIORITY);
        planner.setPriority(MotionPlanner.PRIORITY);

        logger.start(); Thread.sleep(30); safety.start(); planner.start();
        logger.join(); safety.join(); planner.join();

        System.out.println("  Inheritance -> SafetyMonitor waited " + result[0] + " ms");
        return result[0];
    }

    private static long runCeiling() throws InterruptedException {
        long[] result = new long[1];
        PriorityCeilingLock pcLock = new PriorityCeilingLock(Thread.MAX_PRIORITY);

        Thread logger  = new Thread(() -> { pcLock.lock(); try { busySpin(LOGGER_HOLD_MS); } finally { pcLock.unlock(); } }, "ArmLogger");
        Thread safety  = new Thread(() -> {
            sleepMs(SAFETY_DELAY_MS);
            long t0 = System.nanoTime();
            pcLock.lock(); try { result[0] = (System.nanoTime() - t0) / 1_000_000; } finally { pcLock.unlock(); }
        }, "SafetyMonitor");
        Thread planner = new Thread(() -> { sleepMs(PLANNER_DELAY_MS); busySpin(PLANNER_WORK_MS); }, "MotionPlanner");

        logger.setPriority(ArmLogger.PRIORITY);
        safety.setPriority(SafetyMonitor.PRIORITY);
        planner.setPriority(MotionPlanner.PRIORITY);

        logger.start(); Thread.sleep(30); safety.start(); planner.start();
        logger.join(); safety.join(); planner.join();

        System.out.println("  Ceiling     -> SafetyMonitor waited " + result[0] + " ms");
        return result[0];
    }

    private static void busySpin(int ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { Thread.yield(); }
    }

    private static void sleepMs(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static double average(long[] values) {
        long sum = 0;
        for (long v : values) sum += v;
        return (double) sum / values.length;
    }

    private static String formatRuns(long[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values[i]);
        }
        return sb.toString();
    }

    private static void printChart(double baseline, double inheritance, double ceiling) {
        double max = Math.max(baseline, Math.max(inheritance, ceiling));
        int width = 40;
        System.out.println("Bar Chart - SafetyMonitor Average Wait Time:");
        printBar("Baseline   ", baseline, max, width);
        printBar("Inheritance", inheritance, max, width);
        printBar("Ceiling    ", ceiling, max, width);
    }

    private static void printBar(String label, double value, double max, int width) {
        int barLen = (int) Math.round((value / max) * width);
        String bar = "#".repeat(Math.max(0, barLen));
        System.out.printf("%-12s |%-40s| %.1f ms%n", label, bar, value);
    }
}
