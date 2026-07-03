public class ThreadB implements Runnable {

    private SharedVariables sv;

    public ThreadB(SharedVariables sv) {
        this.sv = sv;
    }

    @Override
    public void run() {
        try {
            funcB1();
            funcB2();
            funcB3();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void funcB1() throws InterruptedException {
        sv.semA1done.acquire(); // wait for A1 to finish
        sv.B1 = MathUtils.sum(250);
        System.out.println("[ThreadB] FuncB1 -> B1 = " + sv.B1);
    }

    private void funcB2() throws InterruptedException {
        sv.B2 = MathUtils.sum(200);
        System.out.println("[ThreadB] FuncB2 -> B2 = " + sv.B2);
        sv.semB2done.release(); // signal A2 can start
    }

    private void funcB3() throws InterruptedException {
        sv.semA2done.acquire(); // wait for A2 to finish
        sv.B3 = sv.B2 + MathUtils.sum(400);
        System.out.println("[ThreadB] FuncB3 -> B3 = " + sv.B3);
        sv.semB3done.release(); // signal A3 can start
    }
}
