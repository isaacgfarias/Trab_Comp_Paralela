import java.awt.*;
import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
// import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MergeSort extends JPanel {

    private static int[][] results;
    private static int resultIndex = 0;

    public static void main(String[] args) {
        int[] sampleSizes = {4, 8, 16, 32, 64, 128, 256, 512};
        int[] processorCounts = {3, 6};
        results = new int[sampleSizes.length * processorCounts.length][3]; // [size, processors, time(ns)]

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Merge Sort Benchmark");
            MergeSort panel = new MergeSort();
            frame.add(panel);
            frame.setSize(1000, 650);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            for (int size : sampleSizes) {
                int[] sampleArray = generateRandomArray(size);
                for (int processors : processorCounts) {
                    int time = runSortingTests(sampleArray, processors);
                    panel.updateResults(size, processors, time);
                }
            }

            saveResultsToCSV("MergeSort_results.csv");
        });
    }

    private static int[] generateRandomArray(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = (int) (Math.random() * 100);
        }
        return array;
    }

    private static int runSortingTests(int[] array, int processors) {
        System.out.println("\nArray size: " + array.length + ", Processors: " + processors);

        // Merge Sort Serial
        int[] copyArray = Arrays.copyOf(array, array.length);
        long startTime = System.nanoTime();
        mergeSortSerial(copyArray, 0, copyArray.length - 1);
        long endTime = System.nanoTime();
        int serialTime = (int)(endTime - startTime);
        System.out.println("Merge Sort Serial: " + serialTime + " ns");

        // Merge Sort Parallel
        copyArray = Arrays.copyOf(array, array.length);
        startTime = System.nanoTime();
        parallelMergeSort(copyArray, processors);
        endTime = System.nanoTime();
        int parallelTime = (int)(endTime - startTime);
        System.out.println("Merge Sort Parallel: " + parallelTime + " ns\n");

        return parallelTime;
    }



    
    private void updateResults(int size, int processors, int time) {
        results[resultIndex++] = new int[]{size, processors, time};
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Define dimensions and margins
        int width = getWidth();
        int height = getHeight();
        int margin = 50;

        // Draw title and labels
        g.drawString("Benchmark Results", width / 2 - 40, 20);
        g.drawString("Array Size", width / 2, height - 10);
        g.drawString("Execution Time (ns)", 10, height / 2);

        // Draw X and Y axes
        g.drawLine(margin, height - margin, width - margin, height - margin); // X axis
        g.drawLine(margin, margin, margin, height - margin); // Y axis

        // Axis divisions and labels
        int xDivisions = 7;
        int yDivisions = 8;

        // Get maximum values for scaling
        int maxArraySize = Arrays.stream(results).mapToInt(row -> row[0]).max().orElse(1);
        int maxTime = Arrays.stream(results).mapToInt(row -> row[2]).max().orElse(1);

        // X-axis labels (array sizes)
        for (int i = 0; i <= xDivisions; i++) {
            int x = margin + i * (width - 2 * margin) / xDivisions;
            int arraySize = maxArraySize * i / xDivisions;
            g.drawLine(x, height - margin, x, height - margin + 5);
            g.drawString(String.valueOf(arraySize), x - 10, height - margin + 20);
        }

        // Y-axis labels (execution time)
        for (int i = 0; i <= yDivisions; i++) {
            int y = height - margin - i * (height - 2 * margin) / yDivisions;
            int time = maxTime * i / yDivisions;
            g.drawLine(margin - 5, y, margin, y);
            g.drawString(String.valueOf(time), margin - 35, y + 5);
        }

        // Plot data points
        g.setColor(Color.BLUE);
        for (int i = 1; i < resultIndex; i++) {
            int x1 = margin + (width - 2 * margin) * results[i - 1][0] / maxArraySize;
            int y1 = height - margin - (height - 2 * margin) * results[i - 1][2] / maxTime;
            int x2 = margin + (width - 2 * margin) * results[i][0] / maxArraySize;
            int y2 = height - margin - (height - 2 * margin) * results[i][2] / maxTime;
            g.fillOval(x1 - 3, y1 - 3, 6, 6);
            g.drawLine(x1, y1, x2, y2);  // Connect points with lines
        }
    }

    private static void saveResultsToCSV(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("ArraySize,Processors,Time(ns)\n");
            for (int[] result : results) {
                writer.write(result[0] + "," + result[1] + "," + result[2] + "\n");
            }
            System.out.println("Results saved to " + fileName);
        } catch (IOException e) {
            System.err.println("Error saving results to CSV: " + e.getMessage());
        }
    }




    // Serial Merge Sort
    public static void mergeSortSerial(int[] array, int left, int right) {
        if (left < right) {
            int middle = (left + right) / 2;
            mergeSortSerial(array, left, middle);
            mergeSortSerial(array, middle + 1, right);
            merge(array, left, middle, right);
        }
    }

    private static void merge(int[] array, int left, int middle, int right) {
        int n1 = middle - left + 1;
        int n2 = right - middle;

        int[] leftArray = new int[n1];
        int[] rightArray = new int[n2];

        System.arraycopy(array, left, leftArray, 0, n1);
        System.arraycopy(array, middle + 1, rightArray, 0, n2);

        int i = 0, j = 0;
        int k = left;
        while (i < n1 && j < n2) {
            if (leftArray[i] <= rightArray[j]) {
                array[k] = leftArray[i];
                i++;
            } else {
                array[k] = rightArray[j];
                j++;
            }
            k++;
        }

        while (i < n1) {
            array[k] = leftArray[i];
            i++;
            k++;
        }

        while (j < n2) {
            array[k] = rightArray[j];
            j++;
            k++;
        }
    }

    // Parallel Merge Sort
    public static void parallelMergeSort(int[] array, int processors) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(processors);
        forkJoinPool.invoke(new MergeSortTask(array, 0, array.length - 1));
        forkJoinPool.close();
    }

    static class MergeSortTask extends RecursiveAction {
        private final int[] array;
        private final int left, right;
        private static final int THRESHOLD = 16;

        public MergeSortTask(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (right - left < THRESHOLD) {
                mergeSortSerial(array, left, right);
            } else {
                int middle = (left + right) / 2;
                MergeSortTask leftTask = new MergeSortTask(array, left, middle);
                MergeSortTask rightTask = new MergeSortTask(array, middle + 1, right);
                invokeAll(leftTask, rightTask);
                merge(array, left, middle, right);
            }
        }
    }
}
