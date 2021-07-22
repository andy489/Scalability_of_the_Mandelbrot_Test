import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.*;

public class ManPar {

    protected static int width = 2160, height = 2160;
    protected static int rows = 40, cols = 1, tasks = rows * cols;
    protected static int maxIterations = 1024;
    //protected static double dims[] = {-1.8, 0.45, -1.1, 1.1}; // re left,right; im down, up
    //protected static double dims[]={-0.38-0.01, -0.38+0.01, -0.63-0.01, -0.63+0.01};
    //protected static double k=0.009, l=0.011;
    //protected static double dims[]={-0.793,-0.783,-0.129,-0.119};
    //protected static double dims[]={-0.79,-0.78,-0.14,-0.13};
    protected static double dims[]={-0.7965,-0.7845,-0.1433,-0.1313};
    protected static int taskPixWidth = width / cols, taskPixHeight = height / rows;
    protected static String pathName = "ManPar01.png";
    protected static Thread[] workers;
    protected static int[][] pixels = new int[width][height];
    protected static int threads = 6;
    protected static boolean quiet = false, byCols = false;

    static private void addOptions(String[] args) {
        Options opt = new Options();
        opt.addOption("s", "size", true, "size of image (3840x2160)");
        opt.addOption("r", "rect", true, "dimensions of rectangle in z-plane (-2.0:2.0:-1:0:1.0)");
        opt.addOption("t", "tasks", true, "number of threads (16)");
        opt.addOption("o", "output", true, "output path name (ManPar08.png)");
        opt.addOption("q", "quiet", false, "silent mode");
        opt.addOption("i", "info", false, "arguments information");
        opt.addOption("c", "cols", false, "by rows or by cols");
        opt.addOption("g", "gran", true, "granularity");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(opt, args);
            if (cmd.hasOption("s")) {
                String[] sz = cmd.getOptionValue("s").split("x");
                try {
                    width = Integer.parseInt(sz[0]);
                    height = Integer.parseInt(sz[1]);
                    taskPixWidth = width / cols;
                    taskPixHeight = height / rows;
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
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(5);
                }
            }

            byCols = cmd.hasOption("c");

            if (cmd.hasOption("g")) {
                String gr = cmd.getOptionValue("g");
                try {
                    int gran = Integer.parseInt(gr);
                    if (byCols) {
                        cols = gran * threads;
                        rows = 1;
                    } else {
                        rows = gran * threads;
                        cols = 1;
                    }
                    tasks = rows * cols;
                    taskPixWidth = width / cols;
                    taskPixHeight = height / rows;
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(5);
                }
            }

            if (cmd.hasOption("o")) {
                pathName = cmd.getOptionValue("o");
            }

            quiet = cmd.hasOption("q");

            if (cmd.hasOption("i")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./runMe.sh [OPTIONS]", opt);
                System.exit(6);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(9);
        }
    }

    public static void main(String[] args) {
        addOptions(args);

        int[] colors = new int[maxIterations];

        for (int j = 0; j < maxIterations; ++j) {
            colors[j] = Color.HSBtoRGB(((94 + 1.2f*(float)Math.log(j)*(float)Math.sqrt(j)) / 256f), 0.65f, j / (j + 3.5f));

        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphic = img.createGraphics();
        graphic.fillRect(0, 0, width, height);

        long start = System.currentTimeMillis();

        workers = new Thread[threads];
        for (int j = 1; j < threads; ++j) {
            Runnable r = new Worker(j, quiet, maxIterations, width, height,
                    taskPixWidth, taskPixHeight, rows, cols, tasks, byCols, threads);
            Thread t = new Thread(r);
            t.start();
            workers[j] = t;
        }

        new Worker(0, quiet, maxIterations, width, height,
                taskPixWidth, taskPixHeight, rows, cols, tasks, byCols, threads).run();

        for (int j = 1; j < threads; ++j) {
            try {
                workers[j].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (pixels[x][y] < maxIterations) {
                    img.setRGB(x, y, colors[pixels[x][y]]); // img.setRGB(x, y, getColor(pixels[x][y]));
                    continue;
                }
                img.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        try {
            ImageIO.write(img, "PNG", new File(pathName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        PrintWriter out = new PrintWriter(System.out);
        out.printf("Time elapsed (in millis): " + (end - start) + "\n");

        out.flush();
        out.close();
    }
}
