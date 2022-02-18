import org.apache.commons.math3.complex.Complex;

public class Runnable implements java.lang.Runnable {
    private final int taskLocation;

    public Runnable(int taskLocation) {
        this.taskLocation = taskLocation;
    }

    private Complex calcNewValue(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    private short getIndex(Complex c) {
        Complex z = new Complex(0, 0);

        short currIter = -128;

        while (z.abs() <= 2 && curr_iter < DynamicMandelbrot.MAX_ITERATIONS) {
            z = calcNewValue(z, c);
            currIter++;
        }

        return currIter;
    }

    @Override
    public void run() {
        int startFrom = taskLocation * DynamicMandelbrot.rowWidth;
        int endTo = taskLocation * DynamicMandelbrot.rowWidth + DynamicMandelbrot.rowWidth;
        for (int y = startFrom; y < endTo && y < DynamicMandelbrot.HEIGHT; y++) {
            for (int x = 0; x < DynamicMandelbrot.WIDTH; x++) {
                double pixel_x = DynamicMandelbrot.RE_START + ((double) x / DynamicMandelbrot.WIDTH) * (DynamicMandelbrot.RE_END - DynamicMandelbrot.RE_START);
                double pixel_y = DynamicMandelbrot.IM_START + ((double) y / (DynamicMandelbrot.HEIGHT)) * (DynamicMandelbrot.IM_END - DynamicMandelbrot.IM_START);
                Complex c = new Complex(pixel_x, pixel_y);
            
                DynamicMandelbrot.indices[x][y] = (byte)getIndex(c);
            }
        }
    }
}
