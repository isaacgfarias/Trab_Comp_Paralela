import java.awt.*;
import javax.swing.*;

import java.io.FileWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class SelectionSort extends JPanel {

    private static int[][] results;
    private static int resultIndex = 0;

    public static void main(String[] args) {
        int[] sampleSizes = {4, 8, 16, 32, 64, 128, 256};
        int[] processorCounts = {3, 6, 9};
        results = new int[sampleSizes.length * processorCounts.length][3];

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Selection Sort Benchmark");
            SelectionSort panel = new SelectionSort();
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

            saveResultsToCSV("SelectionSort_resultados.csv");
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
        selectionSortSerial(copyArray);
        long endTime = System.nanoTime();
        System.out.println("Selection Sort Serial: " + (endTime - startTime) + " ns");

        copyArray = Arrays.copyOf(array, array.length);
        startTime = System.nanoTime();
        parallelSelectionSort(copyArray, processors);
        endTime = System.nanoTime();
        System.out.println("Selection Sort Parallel: " + (endTime - startTime) + " ns\n");

        return (int)(endTime - startTime);
    }

    public static void selectionSortSerial(int[] array) {
        int n = array.length;
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (array[j] < array[minIndex]) {
                    minIndex = j;
                }
            }
            int temp = array[minIndex];
            array[minIndex] = array[i];
            array[i] = temp;
        }
    }

    public static void parallelSelectionSort(int[] array, int processors) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(processors);
        forkJoinPool.invoke(new SelectionSortTask(array, 0, array.length));
        forkJoinPool.close();
    }

    static class SelectionSortTask extends RecursiveAction {
        private final int[] array;
        private final int start, end;
        private static final int THRESHOLD = 16;

        public SelectionSortTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                selectionSortSerial(Arrays.copyOfRange(array, start, end));
            } else {
                int mid = (start + end) / 2;
                SelectionSortTask leftTask = new SelectionSortTask(array, start, mid);
                SelectionSortTask rightTask = new SelectionSortTask(array, mid, end);
                invokeAll(leftTask, rightTask);
                merge(array, start, mid, end);
            }
        }
    }

    private static void merge(int[] array, int start, int mid, int end) {
        int[] left = Arrays.copyOfRange(array, start, mid);
        int[] right = Arrays.copyOfRange(array, mid, end);

        int i = 0, j = 0, k = start;
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                array[k++] = left[i++];
            } else {
                array[k++] = right[j++];
            }
        }

        while (i < left.length) {
            array[k++] = left[i++];
        }

        while (j < right.length) {
            array[k++] = right[j++];
        }
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

    private void updateResults(int size, int processors, int time) {
        results[resultIndex++] = new int[]{size, processors, time};
        repaint();
    }


}
