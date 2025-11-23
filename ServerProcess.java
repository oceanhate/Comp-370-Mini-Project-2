import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for all server processes.
 * Contains common server behavior that both Primary and Backup servers share.
 */

public abstract class ServerProcess {
    protected volatile boolean isPrimary = false;
    protected volatile boolean running = true;

    // --- MODIFIED FIELD ---
    // The port number this server instance is running on
    protected final int serverPort;
    
    // --- STATE REPLICATION ---
    // Message counter: tracks total messages processed (replicated to backups)
    protected volatile int messageCount = 0;

    private ServerSocket serverSocket;
    private Thread serverListenThread;
    private Thread heartbeatThread;
    private final Set<Socket> activeClients = Collections.synchronizedSet(new HashSet<>());

    // --- MODIFIED CONSTRUCTOR ---
    // The constructor now accepts the server's port number
    protected ServerProcess(int port) {
        this.serverPort = port;
    }

    /**
     * Starts the server, accepts multiple clients, and handles each in a separate thread.
     */
    public void process() { // Removed 'port' argument as it's now in the constructor
        this.serverListenThread = new Thread(() -> runServer(this.serverPort)); // Use field
        this.serverListenThread.start();

        // Start heartbeat sender as a daemon thread
        this.heartbeatThread = new Thread(this::sendHeartbeats, "heartbeat-sender");
        this.heartbeatThread.setDaemon(true);
        this.heartbeatThread.start();
    }

    // The main server loop: accepts clients and starts a handler thread for each
    private void runServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port: " + port);
            // ... (rest of runServer logic remains the same) ...
            while (running) {
                var client = serverSocket.accept();
                activeClients.add(client);
                Thread clientHandler = new Thread(() -> handleClient(client));
                clientHandler.setDaemon(true);
                clientHandler.start();

            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    // Handles communication with a single client
    private void handleClient(Socket client) {
        try (var clientInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var clientOutput = new PrintWriter(client.getOutputStream(), true)) {
            String line;
            while ((line = clientInput.readLine()) != null) {
                if ("PROMOTE".equals(line)) {
                    isPrimary = true;
                    // Log the promotion success
                    System.out.println("--- RECEIVED PROMOTE COMMAND ---");
                    onPromotedToPrimary(); // Hook for subclasses
                    clientOutput.println("PROMOTED");
                } else if (line.startsWith("STATE_UPDATE:")) {
                    // Backup receiving state update from primary
                    try {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            messageCount = Integer.parseInt(parts[1]);
                            System.out.println("[Backup:" + serverPort + "] State synced. Message count: " + messageCount);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid state update format");
                    }
                } else if (isPrimary) {
                    // Primary processing client message
                    messageCount++; // Increment state
                    System.out.println("[Primary:" + serverPort + "] Client says: " + line + " (Total messages: " + messageCount + ")");
                    
                    // Replicate state to all backup servers
                    replicateStateToBackups();
                    
                    clientOutput.println("Message Received");
                } else {
                    clientOutput.println("NOT PRIMARY (Currently Port: " + serverPort + ")");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected or error occurred.");
        } finally {
            activeClients.remove(client);
        }
    }

    // Runs forever, sending heartbeats
    private void sendHeartbeats() {
        while (running) {
            try {
                sendHeartbeat();
                Thread.sleep(2000); // Wait 2 seconds
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // --- MODIFIED METHOD ---
    // Send one heartbeat message using the server's port number
    private void sendHeartbeat() {
        try (Socket socket = new Socket("localhost", 9000); // Connect to monitor heartbeat port
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            long timeStamp = System.currentTimeMillis();

            // Send the required format: [Port #] | [timestamp]
            out.println(this.serverPort + "|" + timeStamp );

        } catch (IOException e) {
            // Suppress continuous failure logs, only show if the server itself is running.
            if (running) {
                System.out.println("Failed to send heartbeat from port " + this.serverPort);
            }
        }
    }


    /**
     * Hook method for subclasses to override.
     * Called when this server is promoted to primary.
     */
    protected abstract void onPromotedToPrimary();
    
    /**
     * Replicates current state to all backup servers.
     * Sends STATE_UPDATE message to all known backup ports.
     */
    private void replicateStateToBackups() {
        // Get all server ports from the ClusterConfig abstraction
        int[] allPorts = Arrays.stream(ClusterConfig.NODES)
                .mapToInt(node -> node.port)
                .toArray();

        for (int port : allPorts) {
            if (port != this.serverPort) { // Don't send to self
                try (Socket backupSocket = new Socket(ClusterConfig.HOST, port);
                     PrintWriter out = new PrintWriter(backupSocket.getOutputStream(), true)) {
                    out.println("STATE_UPDATE:" + messageCount);
                } catch (IOException e) {
                    // Backup might be down - this is fine, it will sync when promoted
                }
            }
        }
    }

    /**
     * Stops the server process gracefully.
     */
    public void stop() {

        System.out.println("Server on port " + this.serverPort + " shutting down");
        //close listener socket
        this.running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }

        //interupt threads
        if(serverListenThread != null) {
            serverListenThread.interrupt();
        }

        if (this.heartbeatThread != null) {
            this.heartbeatThread.interrupt();
        }

        //wait for threads to exit
        try{
            if(heartbeatThread != null) {
                heartbeatThread.join(1000);
            }
            if(serverListenThread != null) {
                serverListenThread.join(1000);}
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //close client connections
        for(Socket client : activeClients) {
            try {
                client.close();
            } catch (IOException e) {}
        }
        activeClients.clear();
        System.out.println("Server on port " + this.serverPort + " stopped");
    }
}