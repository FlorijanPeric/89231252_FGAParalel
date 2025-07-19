package ForceGraphLayout;

import javax.swing.*;
import java.awt.event.WindowAdapter; // Dodano
import java.awt.event.WindowEvent;     // Dodano
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // Define a class to hold the configuration from StartUi
        class AppConfig {
            int nodes;
            int edges;
            int width;
            int height;
            String mode;

            public AppConfig(int nodes, int edges, int width, int height, String mode) {
                this.nodes = nodes;
                this.edges = edges;
                this.width = width;
                this.height = height;
                this.mode = mode;
            }
        }

        final AppConfig[] config = new AppConfig[1];
        // Latch to wait for StartUi to provide configuration
        CountDownLatch configLatch = new CountDownLatch(1);

        // Run the StartUi on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            StartUi startUi = new StartUi(800, 600, configLatch);
            startUi.createUI();
            // Add an action listener to the save button to capture values
            startUi.getSaveButton().addActionListener(e -> {
                try {
                    config[0] = new AppConfig(
                            startUi.getNumberOfNodes(),
                            startUi.getNumberOfEdges(),
                            startUi.getGraphWidth(),
                            startUi.getGraphHeight(),
                            startUi.getSelectedMode()
                    );
                    configLatch.countDown(); // Release the latch
                    startUi.dispose(); // Close the StartUi window
                } catch (NumberFormatException ex) {
                    // Error message handled in StartUi's action listener
                    // Do not countDown the latch here, let the StartUi remain open
                }
            });

            // Also handle window closing without saving
            startUi.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (configLatch.getCount() > 0) { // If latch hasn't been counted down by save button
                        configLatch.countDown(); // Release the latch even if window is just closed
                    }
                }
            });
        });

        // Wait for the StartUi to provide configuration
        try {
            configLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Configuration retrieval interrupted.");
            return;
        }

        if (config[0] == null) {
            Logger.log("No valid configuration provided or StartUI was closed. Exiting.", LogLevel.Error);
            return;
        }

        int nodeC = config[0].nodes;
        int edgeC = config[0].edges;
        int w = config[0].width;
        int h = config[0].height;
        String mode = config[0].mode;

        Logger.log("Running with configuration:", LogLevel.Info);
        Logger.log("Nodes: " + nodeC, LogLevel.Info);
        Logger.log("Edges: " + edgeC, LogLevel.Info);
        Logger.log("Width: " + w, LogLevel.Info);
        Logger.log("Height: " + h, LogLevel.Info);
        Logger.log("Mode: " + mode, LogLevel.Info);

        int iterations = 1000;
        long seed = 30;
        int seed1 = 40;
        boolean isParallel = true; // Default to parallel execution as 'Type of Run' is removed

        Graph graph = new Graph(nodeC, edgeC, seed, w, h);
        FRAlgorithmParalel alg = new FRAlgorithmParalel(graph, iterations, w, h, seed1, isParallel);

        JFrame frame = null;
        UI panel = null;

        if (mode.equals("UI")) {
            frame = new JFrame("Force Layout Graph");
            panel = new UI(graph);
            frame.add(panel);
            frame.setVisible(true);
            frame.setSize(w, h);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        long start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            if (!alg.run()) {
                Logger.log("Not a good idea to proceed so closing", LogLevel.Success);
                break;
            } else {
                Logger.log("I am working (iteration " + (i + 1) + ")", LogLevel.Info);
                if (mode.equals("UI") && panel != null) {
                    panel.updateGraph();
                    // Optional: Add a small delay for better visual effect in UI mode
                    // Thread.sleep(10);
                }
            }
        }

        long end = System.currentTimeMillis();
        Logger.log("Time used " + (end - start) + " ms, " + ((end - start) / 1000) + " s, " +
                (double) ((end - start) / 1000.0) / 60.0 + " min", LogLevel.Success);

        if (isParallel) {
            Logger.log("We need to close the executor service running parallelism....", LogLevel.Warn);
            alg.closeExec();
            Logger.log("All executors taken care of", LogLevel.Success);
        }

        /*if (frame != null) { // Dispose of the main frame if it was created
            frame.dispose();
        }
         */
    }
}