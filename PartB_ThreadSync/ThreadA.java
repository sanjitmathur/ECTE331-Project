public class ThreadA implements Runnable {

    private SharedVariables sv;

    public ThreadA(SharedVariables sv) {
        this.sv = sv;
    }

    @Override
    public void run() {
        try {
            funcA1();
            funcA2();
            funcA3();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void funcA1() throws InterruptedException {
        sv.A1 = 500;
        System.out.println("[ThreadA] FuncA1 -> A1 = " + sv.A1);
        sv.semA1done.release(); // signal B1 can start
    }

    private void funcA2() throws InterruptedException {
        sv.semB2done.acquire(); // wait for B2 to finish
        sv.A2 = sv.A1 + MathUtils.sum(300);
        System.out.println("[ThreadA] FuncA2 -> A2 = " + sv.A2);
        sv.semA2done.release(); // signal B3 can start
    }

    private void funcA3() throws InterruptedException {
        sv.semB3done.acquire(); // wait for B3 to finish
        sv.A3 = sv.A2 + MathUtils.sum(400);
        System.out.println("[ThreadA] FuncA3 -> A3 = " + sv.A3);
    }
}
