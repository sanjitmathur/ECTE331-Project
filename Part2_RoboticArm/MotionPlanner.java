public class MotionPlanner implements Runnable {

    public static final int PRIORITY = Thread.NORM_PRIORITY;
    private static final int HOLD_MS = 400;

    private MotorController motor;
    private int iterations;
    private String mode;

    public MotionPlanner(MotorController motor, int iterations, String mode) {
        this.motor = motor;
        this.iterations = iterations;
        this.mode = mode;
    }

    @Override
    public void run() {
        MotorController.timestamp("MotionPlanner [MED P=" + Thread.currentThread().getPriority() + "] started");

        for (int i = 0; i < iterations; i++) {
            MotorController.timestamp("MotionPlanner computing trajectory - iteration " + (i + 1));

            if (mode.equals("basic")) {
                motor.accessUnsynchronised("MotionPlanner", HOLD_MS);
            } else if (mode.equals("ceiling")) {
                motor.accessCeiling("MotionPlanner", HOLD_MS);
            } else {
                motor.accessSynchronised("MotionPlanner", HOLD_MS);
            }

            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        MotorController.timestamp("MotionPlanner [MED] finished");
    }
}
