import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Client class implementing Singleton pattern.
 * Ensures only one client instance manages connections to the primary server.
 */
public class Client {

    // --- SINGLETON PATTERN ---
    private static Client instance;
    
    private Client() {}

    public static synchronized Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    // Java entry point — calls singleton instance
    public static void main(String[] args) {
        Client.getInstance().run(args);
    }

    private final String HOST = "localhost";
    private final int MONITOR_API_PORT = 9001; // Monitor's API port
    private final int RECONNECT_INTERVAL = 5000; // 5 seconds

    /**
     * Queries the Monitor for the port of the current Primary server.
     * @return The port number of the current Primary, or 0 if monitor/connection fails.
     */
    private int getPrimaryPortFromMonitor() {
        System.out.println("Querying Monitor for current Primary...");
        try (Socket socket = new Socket(HOST, MONITOR_API_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Set a short read timeout to prevent blocking indefinitely if monitor is silent
            socket.setSoTimeout(2000);

            out.println("GET_PRIMARY");
            String response = in.readLine();

            if (response != null && response.matches("\\d+")) {
                int primaryPort = Integer.parseInt(response);
                System.out.println("Monitor reports Primary is on port: " + primaryPort);
                return primaryPort;
            } else {
                System.out.println("Monitor returned invalid response or error: " + response);
                return 0;
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to Monitor on port " + MONITOR_API_PORT + ". Falling back to fixed list.");
            return 0; // Return 0 on failure
        }
    }

    // Main client loop - renamed from  king main to run for singleton
    public void run(String[] args) {
        Scanner consoleInput = new Scanner(System.in);

        while (true) {
            // 1. QUERY MONITOR FOR CURRENT PRIMARY
            int monitorReportedPrimary = getPrimaryPortFromMonitor();

            if (monitorReportedPrimary <= 0) {
                // Monitor didn't return a valid primary - system may be down
                System.out.println("Monitor did not return a valid primary. Retrying in " + RECONNECT_INTERVAL / 1000 + " seconds...");
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECT_INTERVAL);
                    continue;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 2. CONNECT ONLY TO MONITOR'S DESIGNATED PRIMARY
            // No fallback to other ports - we only connect to what monitor says is primary
            try {
                System.out.println("Attempting connection to primary server on port: " + monitorReportedPrimary);
                connectAndRun(consoleInput, monitorReportedPrimary);
                // If connectAndRun returns without an exception, it means the user typed 'exit' or the server died.
                // Loop back to query monitor for new primary.
            } catch (IOException e) {
                System.out.println("Connection to primary on port " + monitorReportedPrimary + " failed.");
                System.out.println("Waiting for monitor to detect failure and promote new primary...");
                System.out.println("Retrying in " + RECONNECT_INTERVAL / 1000 + " seconds...");
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECT_INTERVAL);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        consoleInput.close();
    }

    private void connectAndRun(Scanner consoleInput, int port) throws IOException {
        try (Socket socket = new Socket(HOST, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server on port " + port + ". Type messages to send. Type 'exit' to quit.");

            // Flag to track if the connection is still considered alive from the perspective of the server handler
            final boolean[] connectionActive = {true};

            // Thread to read messages from the server asynchronously
            Thread readerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while (!Thread.currentThread().isInterrupted() && (serverMessage = in.readLine()) != null) {
                        System.out.println("from server: " + serverMessage);
                    }
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        System.out.println("Server connection lost. Retrying...");
                    }
                } finally {
                    connectionActive[0] = false; // Mark connection as dead
                    // Interrupt the main client send loop to stop accepting user input
                    Thread.currentThread().interrupt();
                }
            });

            readerThread.setDaemon(true);
            readerThread.start();

            boolean userExited = false;
            // Main loop to send user input
            while (connectionActive[0] && !Thread.interrupted()) { // CRITICAL CHECK

                // Block until there's input
                if (!consoleInput.hasNextLine()) {
                    // This typically only happens if System.in is closed, but good safety check
                    break;
                }

                String message = consoleInput.nextLine();

                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Client disconnecting by user.");
                    userExited = true;
                    break;
                }

                out.println(message);
                out.flush();

                if (out.checkError()) {
                    // **FIXED LOGIC**: If a send error occurs (socket failure), break immediately.
                    System.out.println("Connection lost during send operation.");
                    break;
                }
            }

            // Clean up the reader thread
            readerThread.interrupt();
            try {
                readerThread.join(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            // If the user did NOT explicitly exit, it means the connection died.
            if (!userExited) {
                // Throw an exception to tell the main loop to try the next port/reconnect.
                throw new IOException("Connection ended unexpectedly, forcing reconnect attempt.");
            }
        } finally {
            // Print reader thread finished ONLY when the main thread fully exits the try-with-resources
            System.out.println("Reader thread finished.");
        }
    }
}