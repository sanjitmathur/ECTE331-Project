public class ThreadSyncApp {

    private static final int VERIFICATION_RUNS = 1000;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Part B: Thread Synchronisation and Communication");
        System.out.println("Using Semaphores for inter-thread dependency enforcement\n");

        // show expected values
        System.out.println("Expected values:");
        System.out.println("  A1 = " + SharedVariables.EXPECTED_A1);
        System.out.println("  B1 = sum(250) = " + SharedVariables.EXPECTED_B1);
        System.out.println("  B2 = sum(200) = " + SharedVariables.EXPECTED_B2);
        System.out.println("  A2 = A1 + sum(300) = " + SharedVariables.EXPECTED_A2);
        System.out.println("  B3 = B2 + sum(400) = " + SharedVariables.EXPECTED_B3);
        System.out.println("  A3 = A2 + sum(400) = " + SharedVariables.EXPECTED_A3);
        System.out.println();

        // single demonstration run
        System.out.println("--- Single Run ---");
        SharedVariables sv = new SharedVariables();
        Thread tA = new Thread(new ThreadA(sv), "ThreadA");
        Thread tB = new Thread(new ThreadB(sv), "ThreadB");
        tA.start();
        tB.start();
        tA.join();
        tB.join();

        System.out.println("\nComputed:");
        System.out.println("  A1=" + sv.A1 + " B1=" + sv.B1 + " B2=" + sv.B2);
        System.out.println("  A2=" + sv.A2 + " B3=" + sv.B3 + " A3=" + sv.A3);
        System.out.println("Result: " + (sv.verify() ? "CORRECT" : "WRONG"));

        // high-iteration verification (Part B-d)
        System.out.println("\n--- Running " + VERIFICATION_RUNS + " iterations for verification ---");
        int passed = 0;
        int failed = 0;

        for (int i = 0; i < VERIFICATION_RUNS; i++) {
            SharedVariables sv2 = new SharedVariables();
            Thread a = new Thread(new ThreadA(sv2));
            Thread b = new Thread(new ThreadB(sv2));
            a.start(); b.start();
            a.join(); b.join();
            if (sv2.verify()) passed++;
            else failed++;
        }

        System.out.println("Total: " + VERIFICATION_RUNS + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println(failed == 0 ? "All correct - synchronisation is deterministic." : "Failures detected!");
    }
}
