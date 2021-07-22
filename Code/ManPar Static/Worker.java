import org.apache.commons.math3.complex.Complex;

public class Worker implements java.lang.Runnable {

    private int indThread;
    private boolean quiet, byCols;
    private int maxIterations;
    private int width, height;
    private double[] dims = ManPar.dims;
    private int rows, cols;
    private int taskPixWidth, taskPixHeight, tasks, threads;

    public Worker(int indThread, boolean quiet, int maxIterations,
                  int width, int height, int taskPixWidth, int taskPixHeight,
                  int rows, int cols, int tasks, boolean byCols, int threads) {
        this.indThread = indThread;
        this.quiet = quiet;
        this.maxIterations = maxIterations;
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.cols = cols;
        this.taskPixWidth = taskPixWidth;
        this.taskPixHeight = taskPixHeight;
        this.tasks = tasks;
        this.byCols = byCols;
        this.threads = threads;
    }

    private Complex calcNext(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    private int getIter(Complex c) {
        Complex z = new Complex(0, 0);

        int currIter = 0;

        while (z.abs() <= 2 && currIter < maxIterations) {
            z = calcNext(z, c);
            ++currIter;
        }
        return currIter;
    }

    public void byRows() {
        for (int task = indThread; task < tasks; task += threads) {
            int p = (task % rows) * taskPixHeight;
            for (int x = p; x < p + taskPixHeight; ++x) {
                int q = (task / rows) * taskPixWidth;
                for (int y = q; y < q + taskPixWidth; ++y) {
                    double pixel_x = dims[2] + ((double) x / height) * (dims[3] - dims[2]),
                            pixel_y = dims[0] + ((double) y / width) * (dims[1] - dims[0]);

                    Complex c = new Complex(pixel_y, pixel_x);

                    ManPar.pixels[y][x] = getIter(c);
                }
            }
        }
    }

    public void byCols() {
        for (int task = indThread; task < tasks; task += threads) {
            int p = (task % cols) * taskPixWidth;
            for (int x = p; x < p + taskPixWidth; ++x) {
                int q = (task / cols) * taskPixHeight;
                for (int y = q; y < q + taskPixHeight; ++y) {
                    double pixel_x = dims[0] + ((double) x / width) * (dims[1] - dims[0]),
                            pixel_y = dims[2] + ((double) y / height) * (dims[3] - dims[2]);

                    Complex c = new Complex(pixel_x, pixel_y);

                    ManPar.pixels[x][y] = getIter(c);
                }
            }
        }
    }

    @Override
    public void run() {
        if (!quiet) {
            System.out.println("Thread-" + indThread + " started.");
        }
        long startTime = System.currentTimeMillis();

        if (byCols) {
            byCols();
        } else {
            byRows();
        }

        if (!quiet) {
            long endTime = System.currentTimeMillis();
            System.out.println("Thread-" + indThread + " finished. Execution time was (millis): " + (endTime - startTime));
        }
    }
}
