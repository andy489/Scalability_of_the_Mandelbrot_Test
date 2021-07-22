import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.cli.*;

public class ThreadPoolMain {

    protected static int width = 2160, height = 2160;
    protected static double dims[] = {-1.8, 0.45, -1.1, 1.1}; //Re left-right, Im bottom top;
    protected static int threads = 1, tasks = 1, gran = 1, maxIterations = 1024;
    protected static int[][] pixels = new int[width][height];
    protected static String pathName = "Mandelbrot2022.png";
    protected static boolean quiet = false, byCols = false;
    protected static int numFinishedTasks = 0;

    protected static synchronized void cnt() {
        ++numFinishedTasks;
    }

    protected static long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    static public void addOptions(String[] args) {
        Options opt = new Options();
        opt.addOption("s", "size", true, "size of image (2160x2160)");
        opt.addOption("r", "rect", true, "dimensions of rectangle in z-plane (-2.0:2.0:-1:0:1.0)");
        opt.addOption("t", "tasks", true, "number of threads (16)");
        opt.addOption("o", "output", true, "output path name (ManPar08.png)");
        opt.addOption("q", "quiet", false, "silent mode");
        opt.addOption("i", "info", false, "arguments information");
        opt.addOption("g", "gran", true, "granularity");
        opt.addOption("c", "cols", false, "Decomposition byCols");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(opt, args);
            if (cmd.hasOption("s")) {
                String[] sz = cmd.getOptionValue("s").split("x");
                try {
                    width = Integer.parseInt(sz[0]);
                    height = Integer.parseInt(sz[1]);
                    pixels = new int[width][height];
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(1);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(e.getMessage());
                    System.exit(2);
                }
            }

            if (cmd.hasOption("r")) {
                String[] dim = cmd.getOptionValue("r").split(":");
                try {
                    for (int j = 0; j < dim.length; ++j) {
                        dims[j] = Float.parseFloat(dim[j]);
                    }
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(3);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(e.getMessage());
                    System.exit(4);
                }
            }

            if (cmd.hasOption("t")) {
                String workers = cmd.getOptionValue("t");
                try {
                    threads = Integer.parseInt(workers);
                    tasks = gran * threads;
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(5);
                }
            }

            if (cmd.hasOption("g")) {
                String g = cmd.getOptionValue("g");
                try {
                    gran = Integer.parseInt(g);
                    tasks = gran * threads;
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(6);
                }
            }

            if (cmd.hasOption("o")) {
                pathName = cmd.getOptionValue("o");
            }

            quiet = cmd.hasOption("q");
            byCols = cmd.hasOption("c");

            if (cmd.hasOption("i")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./runMe.sh [OPTIONS]", opt);
                System.exit(7);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(8);
        }
    }

    static private Complex calcNext(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    static private int getIter(Complex c) {
        Complex z = new Complex(0, 0);

        int currIter = 0;

        while (z.abs() <= 2 && currIter < maxIterations) {
            z = calcNext(z, c);
            ++currIter;
        }
        return currIter;
    }

    static private void byRows(int kk, int part) {
        for (int y = kk; y < kk + part && y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double pixel_x = dims[0] + ((double) x / width) * (dims[1] - dims[0]),
                        pixel_y = dims[2] + ((double) y / height) * (dims[3] - dims[2]);

                Complex c = new Complex(pixel_x, pixel_y);

                pixels[x][y] = getIter(c);
            }
        }
    }

    static private void byCols(int kk, int part) {
        for (int x = kk; x < kk + part && x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                double pixel_x = dims[0] + ((double) x / width) * (dims[1] - dims[0]),
                        pixel_y = dims[2] + ((double) y / height) * (dims[3] - dims[2]);

                Complex c = new Complex(pixel_x, pixel_y);

                pixels[x][y] = getIter(c);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        addOptions(args);

        int[] colors = new int[maxIterations];
        for (int i = 0; i < maxIterations; ++i) {
            colors[i] = Color.HSBtoRGB((176 + 0.5f * i) / 256, 0.65f, i / (i + 3.5f));
        }

        BufferedImage myImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = myImage.createGraphics();
        g2.fillRect(0, 0, width, height);

        long startTime = getTimeInMillis();

        ThreadPool threadPool = new ThreadPool(threads - 1, tasks);

        int div = tasks;
        // div = 4; //
        int partRows = height / div + (height % div == 0 ? 0 : 1);
        int partCols = width / div + (width % div == 0 ? 0 : 1);
        final int part = byCols ? partCols : partRows;
        for (int k = 0; k < div; ++k) {
            // if (k == 0 || k == 2) continue;
            final int kk = k * part;
            threadPool.execute(() -> {
                if (byCols) {
                    byCols(kk, part);
                } else {
                    byRows(kk, part);
                }
                cnt();
            });
        }

        threadPool.waitUntilAllTasksFinished();
        threadPool.stop(quiet);

        try {
            while (numFinishedTasks < tasks) { // <2
                Thread.sleep(20);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long endTime = getTimeInMillis();

        threadPool.killThreads();

        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                if (pixels[i][j] == maxIterations) {
                    myImage.setRGB(i, j, Color.WHITE.getRGB());
                } else {
                    myImage.setRGB(i, j, colors[pixels[i][j]]);
                }
            }
        }

        // g2.drawRect(0, 0, width - 2, height - 2);

        try {
            ImageIO.write(myImage, "PNG", new File(pathName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Time elapsed (in millis): " + (endTime - startTime) + "\n");
    }
}
