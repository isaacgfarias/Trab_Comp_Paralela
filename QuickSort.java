import java.awt.*;
import javax.swing.*;

import java.io.FileWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class QuickSort extends JPanel {

    private static int[][] results;
    private static int resultIndex = 0;

    public static void main(String[] args) {
        int[] sampleSizes = {4, 8, 16, 32, 64, 128, 256};
        int[] processorCounts = {3, 6, 9};
        results = new int[sampleSizes.length * processorCounts.length][3];

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Quick Sort Benchmark");
            QuickSort panel = new QuickSort();
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

            saveResultsToCSV("QuickSort_resultados.csv");
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

        int[] copyArray = Arrays.copyOf(array, array.length);
        long startTime = System.nanoTime();
        quickSortSerial(copyArray, 0, copyArray.length - 1);
        long endTime = System.nanoTime();
        System.out.println("Quick Sort Serial: " + (endTime - startTime) + " ns");

        copyArray = Arrays.copyOf(array, array.length);
        startTime = System.nanoTime();
        parallelQuickSort(copyArray, processors);
        endTime = System.nanoTime();
        System.out.println("Quick Sort Parallel: " + (endTime - startTime) + " ns\n");
        
        return (int)(endTime - startTime);
    }

    public static void quickSortSerial(int[] array, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(array, low, high);
            quickSortSerial(array, low, pivotIndex - 1);
            quickSortSerial(array, pivotIndex + 1, high);
        }
    }

    private static int partition(int[] array, int low, int high) {
        int pivot = array[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (array[j] < pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        int temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;
        return i + 1;
    }

    public static void parallelQuickSort(int[] array, int processors) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(processors);
        forkJoinPool.invoke(new QuickSortTask(array, 0, array.length - 1));
        forkJoinPool.close();
    }

    static class QuickSortTask extends RecursiveAction {
        private final int[] array;
        private final int low, high;
        private static final int THRESHOLD = 16; // Para arrays pequenos, é mais eficiente fazer o sort serialmente

        public QuickSortTask(int[] array, int low, int high) {
            this.array = array;
            this.low = low;
            this.high = high;
        }

        @Override
        protected void compute() {
            if (low < high) {
                if (high - low < THRESHOLD) {
                    quickSortSerial(array, low, high); // Use serial para pequenas partições
                } else {
                    int pivotIndex = partition(array, low, high);
                    QuickSortTask leftTask = new QuickSortTask(array, low, pivotIndex - 1);
                    QuickSortTask rightTask = new QuickSortTask(array, pivotIndex + 1, high);
                    invokeAll(leftTask, rightTask);
                }
            }
        }
    }


    private void updateResults(int size, int processors, int time) {
        results[resultIndex++] = new int[]{size, processors, time};
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();
        int margin = 50;

        g.drawString("Benchmark Results", width / 2 - 40, 20);
        g.drawString("Array Size", width / 2, height - 10);
        g.drawString("Execution Time (ns)", 10, height / 2);

        g.drawLine(margin, height - margin, width - margin, height - margin);
        g.drawLine(margin, margin, margin, height - margin);

        int xDivisions = 7;
        int yDivisions = 8;

        int maxArraySize = Arrays.stream(results).mapToInt(row -> row[0]).max().orElse(1);
        int maxTime = Arrays.stream(results).mapToInt(row -> row[2]).max().orElse(1);

        for (int i = 0; i <= xDivisions; i++) {
            int x = margin + i * (width - 2 * margin) / xDivisions;
            int arraySize = maxArraySize * i / xDivisions;
            g.drawLine(x, height - margin, x, height - margin + 5);
            g.drawString(String.valueOf(arraySize), x - 10, height - margin + 20);
        }

        for (int i = 0; i <= yDivisions; i++) {
            int y = height - margin - i * (height - 2 * margin) / yDivisions;
            int time = maxTime * i / yDivisions;
            g.drawLine(margin - 5, y, margin, y);
            g.drawString(String.valueOf(time), margin - 35, y + 5);
        }

        g.setColor(Color.BLUE);
        for (int i = 1; i < resultIndex; i++) {
            int x1 = margin + (width - 2 * margin) * results[i - 1][0] / maxArraySize;
            int y1 = height - margin - (height - 2 * margin) * results[i - 1][2] / maxTime;
            int x2 = margin + (width - 2 * margin) * results[i][0] / maxArraySize;
            int y2 = height - margin - (height - 2 * margin) * results[i][2] / maxTime;
            g.fillOval(x1 - 3, y1 - 3, 6, 6);
            g.drawLine(x1, y1, x2, y2);
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

}
