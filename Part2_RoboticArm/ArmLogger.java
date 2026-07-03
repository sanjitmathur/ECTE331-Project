public class ArmLogger implements Runnable {

    public static final int PRIORITY = Thread.MIN_PRIORITY;
    private static final int HOLD_MS = 600;

    private MotorController motor;
    private int iterations;
    private String mode;

    public ArmLogger(MotorController motor, int iterations, String mode) {
        this.motor = motor;
        this.iterations = iterations;
        this.mode = mode;
    }

    @Override
    public void run() {
        MotorController.timestamp("ArmLogger [LOW P=" + Thread.currentThread().getPriority() + "] started");

        for (int i = 0; i < iterations; i++) {
            MotorController.timestamp("ArmLogger recording activity - iteration " + (i + 1));

            if (mode.equals("basic")) {
                motor.accessUnsynchronised("ArmLogger", HOLD_MS);
            } else if (mode.equals("ceiling")) {
                motor.accessCeiling("ArmLogger", HOLD_MS);
            } else {
                motor.accessSynchronised("ArmLogger", HOLD_MS);
            }

            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        MotorController.timestamp("ArmLogger [LOW] finished");
    }
}
