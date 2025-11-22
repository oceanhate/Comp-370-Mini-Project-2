import java.util.Scanner;

/**
 * The Main class is the entry point for the server application.
 */
public class Main {
    // Define static ports for clarity and easier management.
    public static final int PRIMARY_PORT = 8090;
    public static final int BACKUP_PORT = 8089;
    public static final int BACKUP_PORT2 = 8088;

    /**
     * The main method creates and starts the primary and backup servers, each in its own thread.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // --- 1. CONSTRUCTOR ALIGNMENT ---
        // Pass the port number to the server constructor so the server knows its own port.
        var primaryServer = new ServerNode(PRIMARY_PORT, ServerNode.Role.PRIMARY);
        var backupServer = new ServerNode(BACKUP_PORT, ServerNode.Role.BACKUP);
        var backupServer2 = new ServerNode(BACKUP_PORT2, ServerNode.Role.BACKUP);

        // Announce that the servers are about to start.
        System.out.println("Server processes starting...");

        // --- 2. PROCESS CALL ALIGNMENT ---
        // The process() method no longer needs the port argument, as it uses the port stored in the constructor.
        primaryServer.process();
        System.out.println("Primary server started on port " + PRIMARY_PORT);

        backupServer.process();
        System.out.println("Backup server started on port " + BACKUP_PORT);

        backupServer2.process();
        System.out.println("Backup server 2 started on port " + BACKUP_PORT2);

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter a command (stop primary / stop backup1 / stop backup2 / stop all / exit / reboot): \n");
                String command = scanner.nextLine().trim().toLowerCase();

                switch (command) {
                    case "stop primary" -> {
                        primaryServer.stop();
                        System.out.println("Primary server stopped");
                    }
                    case "stop backup1" -> {
                        backupServer.stop();
                        System.out.println("Backup server1 stopped");
                    }
                    case "stop backup2" -> {
                        backupServer2.stop();
                        System.out.println("Backup server2 stopped");
                    }
                    case "stop all" -> {
                        primaryServer.stop();
                        backupServer.stop();
                        backupServer2.stop();
                        System.out.println("All servers stopped.");
                    }
                    case "reboot" -> {
                        System.out.println("Rebooting all servers...");
                        primaryServer.stop();
                        backupServer.stop();
                        backupServer2.stop();

                        // Give time for sockets to close
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

                        // Re-initialize with correct ports
                        primaryServer = new ServerNode(PRIMARY_PORT, ServerNode.Role.PRIMARY);
                        backupServer = new ServerNode(BACKUP_PORT, ServerNode.Role.BACKUP);
                        backupServer2 = new ServerNode(BACKUP_PORT2, ServerNode.Role.BACKUP);

                        // Call process() without arguments
                        primaryServer.process();
                        backupServer.process();
                        backupServer2.process();
                        System.out.println("All servers rebooted successfully.");
                    }

                    case "exit" -> {
                        System.out.println("Shutting down...");
                        primaryServer.stop();
                        backupServer.stop();
                        backupServer2.stop();
                        System.exit(0);

                    }
                }
            }
        }
    }
}