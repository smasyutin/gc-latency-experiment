import java.util.Arrays;

public class Main {

    // batch size for JVM taken from this article
    // https://noelwelsh.com/posts/2020-02-13-benchmarking-graal-native-image.html
    private static final int warmUpBatchSize = 10_000;
    private static final int msgSize = 1024;

    private static byte[] createMessage(final int n) {
        final byte[] msg = new byte[msgSize];
        Arrays.fill(msg, (byte) n);
        return msg;
    }

    private static long pushMessage(final byte[][] c, final int id) {
        final long start = System.nanoTime();
        byte[] m = createMessage(id);
        c[id] = m;
        return System.nanoTime() - start;
    }

    private static void runBatch(int msgCount, int windowSize) {
        System.out.println("Message count: " + msgCount);
        System.out.println("Bugger size: " + windowSize);
        final Stats stats = new Stats(msgCount);

        final long start = System.nanoTime();
        final byte[][] c = new byte[windowSize][msgSize];
        final long allocated = System.nanoTime() - start;
        for (int i = 0; i < msgCount; i++) {
            stats.record(
                    pushMessage(c, i % windowSize)
            );
        }
        System.out.println("Allocation time: " + (allocated / 1000) + "µs");
        final long elapsed = System.nanoTime() - start;
        System.out.println("Push time stats:\n" + stats);
        System.out.println("Total time: " + ((float) elapsed / 1_000_000_000) + "s");
    }

    private static void warmUp(int windowSize) {
        System.out.println("Warming-up...");
        runBatch(warmUpBatchSize, windowSize);
        System.out.println("...Warmed-up");
    }

    public static void main(String[] args) {
        final int msgCount = Integer.parseInt(args[0]);
        final int windowSize = Integer.parseInt(args[1]);

        warmUp(windowSize);
        runBatch(msgCount, windowSize);
    }

    private static class Stats {
        int msgCount;
        long max = 0;
        long[] histogram = new long[500_000];

        public Stats(int msgCount) {
            this.msgCount = msgCount;
        }

        void record(long tNano) {
            int tMicro = (int)(tNano / 1000);
            if (tMicro > max) {
                max = tMicro;
            }
            int index = tMicro;
            if (index >= histogram.length) {
                index = histogram.length -1;
            }
            histogram[index]++;
        }

        long percentile(double percent) {
            int valuesCount = (int) (msgCount * percent);
            int index = 0;
            for (int n = 0; (index < histogram.length) && (n < valuesCount); index++) {
                n += histogram[index];
            }

            return index;
        }

        Long countLongerThanMs(int ms) {
            long result = 0;
            for (int i = (ms * 1000); i < histogram.length; i++) {
                result += histogram[i];
            }
            return result;
        }

        public String toString() {
            return "max: " + (max / 1000) + "ms" +
                    "\n68.3 percentile: " + percentile(0.683) + "µs" +
                    "\n95.4 percentile: " + percentile(0.954) + "µs" +
                    "\n99.7 percentile: " + percentile(0.997) + "µs" +
                    "\n" + countLongerThanMs(10) + " longer than 10ms" +
                    "\n" + countLongerThanMs(30) + " longer than 30ms" +
                    "\n" + countLongerThanMs(100) + " longer than 100ms";
        }
    }
}
