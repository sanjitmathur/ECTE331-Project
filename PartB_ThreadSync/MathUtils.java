public class MathUtils {

    // computes sum of integers from 0 to n using a loop
    public static long sum(int n) {
        long total = 0;
        for (int i = 0; i <= n; i++) {
            total += i;
        }
        return total;
    }
}
