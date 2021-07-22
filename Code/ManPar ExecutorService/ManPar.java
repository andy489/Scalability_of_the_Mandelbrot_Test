import org.apache.commons.cli.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class DynamicMandelbrot {
    static int WIDTH = 3840, HEIGHT = 2160, MAX_ITERATIONS = 1024;
    static float RE_START = -2f, RE_END = 2f, IM_START = -2f, IM_END = 2f;
    static byte[][] indices;
    static int numThreads = 1, granularity = 1, rows, rowWidth;
    static String picName = "Mandelbrot.png";

    protected static long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    static void insertOptions(String[] args) {
        Options options = new Options();
        options.addOption("r", "rect", true, "Define the complex plane borders. They default to -2.0:2.0 and -2.0:2.0 for the real and imaginary part.");
        options.addOption("t", "tasks", true, "Number of threads to start for the execution of the Mandelbrot set. Defaults to 1 if not provided.");
        options.addOption("o", "output", true, "The name of the picture where the output should be stored. Defaults to 'Mandelbrot' if not provided.");
        options.addOption("s", "size", true, "The size of the output picture in the form <width>x<height>. The default values are 3840x2160");
        options.addOption("h", "help", false, "Displays information for the available commands.");
        options.addOption("g", "granularity", true, "Defines how many tasks per thread we want. Defaults to 1.");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./runMandelbrot.sh [OPTIONS]", options);
                System.exit(0);
            }

            if (cmd.hasOption("t")) {
                try {
                    numThreads = Integer.parseInt(cmd.getOptionValue("t"));
                }
                catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                }
            }

            if (cmd.hasOption("o")) {
                picName = cmd.getOptionValue("o");
            }

            if (cmd.hasOption("r")) {
                String[] rectPoints = cmd.getOptionValue("r").split(":");
                try {
                    RE_START = Float.parseFloat(rectPoints[0]);
                    RE_END = Float.parseFloat(rectPoints[1]);
                    IM_START = Float.parseFloat(rectPoints[2]);
                    IM_END = Float.parseFloat(rectPoints[3]);
                }
                catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Array index out of bounds at: " + e.getMessage() + ". Please make sure to provide 4 values to '-r' option.");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("s")) {
                String[] sizeParams = cmd.getOptionValue("s").split("x");
                try {
                    WIDTH = Integer.parseInt(sizeParams[0]);
                    HEIGHT = Integer.parseInt(sizeParams[1]);
                }
                catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Array index out of bounds at: " + e.getMessage() + ". Please make sure to provide 2 values to '-s' option.");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("g")) {
                try {
                    granularity = Integer.parseInt(cmd.getOptionValue("g"));
                }
                catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                }
            }
        }
        catch(ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        insertOptions(args);
	indices = new byte[WIDTH][HEIGHT];
        rows = numThreads * granularity;
        rowWidth = (int) Math.ceil((float)HEIGHT / rows);

        int[] colors = new int[MAX_ITERATIONS];
        for (int i = 0; i<MAX_ITERATIONS; i++) {
            colors[i] = Color.HSBtoRGB((200f + i * 2f)/256f, 1, i/(i+8f));
        }

        colors[127] = Color.BLACK.getRGB();

        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bi.createGraphics();
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        long startTime = getTimeInMillis();

        ExecutorService pool = Executors.newFixedThreadPool(numThreads - 1);

        Runnable[] tasks = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            tasks[i] = new Runnable(i);
        }

        for (int i = 0; i < rows; i++) {
            pool.execute(tasks[i]);
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = getTimeInMillis();

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (indices[x][y] < 0) {
                    indices[x][y] += 128;
                }
                bi.setRGB(x, y, colors[indices[x][y]]);
            }
        }

        try {
            ImageIO.write(bi, "PNG", new File(picName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Total execution time: " + (endTime - startTime) + " ms.");
    }
}
