public class Task2_Synchronization {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Task 2: Synchronisation - Mutual Exclusion");
        System.out.println("Using synchronized keyword on MotorController\n");

        MotorController motor = new MotorController();
        int iterations = 2;

        Thread highThread = new Thread(new SafetyMonitor(motor, iterations, "sync"), "SafetyMonitor");
        Thread medThread  = new Thread(new MotionPlanner(motor, iterations, "sync"), "MotionPlanner");
        Thread lowThread  = new Thread(new ArmLogger(motor, iterations, "sync"), "ArmLogger");

        highThread.setPriority(SafetyMonitor.PRIORITY);
        medThread.setPriority(MotionPlanner.PRIORITY);
        lowThread.setPriority(ArmLogger.PRIORITY);

        highThread.start();
        medThread.start();
        lowThread.start();

        highThread.join();
        medThread.join();
        lowThread.join();

        System.out.println("\nTask 2 complete. ENTER/EXIT messages should not interleave.");
    }
}
