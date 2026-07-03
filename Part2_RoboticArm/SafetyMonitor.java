public class SafetyMonitor implements Runnable {

    public static final int PRIORITY = Thread.MAX_PRIORITY;
    private static final int HOLD_MS = 300;

    private MotorController motor;
    private int iterations;
    private String mode;
    private volatile long acquireAttemptNs = -1;
    private volatile long acquiredNs = -1;

    public SafetyMonitor(MotorController motor, int iterations, String mode) {
        this.motor = motor;
        this.iterations = iterations;
        this.mode = mode;
    }

    @Override
    public void run() {
        MotorController.timestamp("SafetyMonitor [HIGH P=" + Thread.currentThread().getPriority() + "] started");

        for (int i = 0; i < iterations; i++) {
            MotorController.timestamp("SafetyMonitor checking emergency - iteration " + (i + 1));
            acquireAttemptNs = System.nanoTime();

            if (mode.equals("basic")) {
                motor.accessUnsynchronised("SafetyMonitor", HOLD_MS);
            } else if (mode.equals("ceiling")) {
                motor.accessCeiling("SafetyMonitor", HOLD_MS);
            } else {
                motor.accessSynchronised("SafetyMonitor", HOLD_MS);
            }

            acquiredNs = System.nanoTime();
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        MotorController.timestamp("SafetyMonitor [HIGH] finished");
    }

    public long getWaitTimeMs() {
        if (acquireAttemptNs < 0 || acquiredNs < 0) return -1;
        return (acquiredNs - acquireAttemptNs) / 1_000_000;
    }
}
