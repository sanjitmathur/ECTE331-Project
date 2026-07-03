public class Task1_BasicImpl {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Task 1: Basic Multi-threaded Implementation");
        System.out.println("No synchronisation - threads may interleave\n");

        MotorController motor = new MotorController();
        int iterations = 2;

        Thread highThread = new Thread(new SafetyMonitor(motor, iterations, "basic"), "SafetyMonitor");
        Thread medThread  = new Thread(new MotionPlanner(motor, iterations, "basic"), "MotionPlanner");
        Thread lowThread  = new Thread(new ArmLogger(motor, iterations, "basic"), "ArmLogger");

        highThread.setPriority(SafetyMonitor.PRIORITY);
        medThread.setPriority(MotionPlanner.PRIORITY);
        lowThread.setPriority(ArmLogger.PRIORITY);

        System.out.println("Priorities: SafetyMonitor=" + highThread.getPriority()
                + " MotionPlanner=" + medThread.getPriority()
                + " ArmLogger=" + lowThread.getPriority() + "\n");

        highThread.start();
        medThread.start();
        lowThread.start();

        highThread.join();
        medThread.join();
        lowThread.join();

        System.out.println("\nTask 1 complete.");
    }
}
