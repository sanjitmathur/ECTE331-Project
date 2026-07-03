import java.util.concurrent.Semaphore;

public class SharedVariables {

    // shared result variables
    public volatile long A1 = 0;
    public volatile long A2 = 0;
    public volatile long A3 = 0;
    public volatile long B1 = 0;
    public volatile long B2 = 0;
    public volatile long B3 = 0;

    // semaphores for enforcing execution dependencies (initial count 0 = blocked)
    public final Semaphore semA1done = new Semaphore(0); // FuncA1 done -> FuncB1 can start
    public final Semaphore semB2done = new Semaphore(0); // FuncB2 done -> FuncA2 can start
    public final Semaphore semA2done = new Semaphore(0); // FuncA2 done -> FuncB3 can start
    public final Semaphore semB3done = new Semaphore(0); // FuncB3 done -> FuncA3 can start

    // expected correct values for verification
    public static final long EXPECTED_A1 = 500L;
    public static final long EXPECTED_B1 = MathUtils.sum(250);
    public static final long EXPECTED_B2 = MathUtils.sum(200);
    public static final long EXPECTED_A2 = EXPECTED_A1 + MathUtils.sum(300);
    public static final long EXPECTED_B3 = EXPECTED_B2 + MathUtils.sum(400);
    public static final long EXPECTED_A3 = EXPECTED_A2 + MathUtils.sum(400);

    public boolean verify() {
        return A1 == EXPECTED_A1 && B1 == EXPECTED_B1 && B2 == EXPECTED_B2
            && A2 == EXPECTED_A2 && B3 == EXPECTED_B3 && A3 == EXPECTED_A3;
    }

    public void reset() {
        A1 = A2 = A3 = B1 = B2 = B3 = 0;
    }
}
