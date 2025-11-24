/**
 * Represents a server node (PRIMARY or BACKUP) inside the SRMS cluster.
 * Uses Abstraction–Occurrence: the abstraction is ServerProcess, and each
 * occurrence is a ServerNode instance distinguished by its Role + port.
 */
public class ServerNode extends ServerProcess {

    public enum Role {
        PRIMARY,
        BACKUP
    }

    private Role role;

    public ServerNode(int port, Role role) {
        super(port);
        this.role = role;

        // If this node is PRIMARY at startup, reflect that in the base class state.
        if (role == Role.PRIMARY) {
            this.isPrimary = true;
        }
    }

    /**
     * Called automatically when the Monitor promotes this node to PRIMARY.
     * We update the local role and print a notification.
     */
    @Override
    protected void onPromotedToPrimary() {
        this.role = Role.PRIMARY;
        this.isPrimary = true;

        System.out.println("[PROMOTION] Server on port "
                + this.serverPort + " has been promoted to PRIMARY.");
    }

    /**
     * A small helper method for debugging/logging.
     */
    public boolean isPrimaryRole() {
        return this.role == Role.PRIMARY;
    }

    public Role getRole() {
        return this.role;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ServerNode <port> <PRIMARY|BACKUP>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        Role role = Role.valueOf(args[1].toUpperCase());

        ServerNode node = new ServerNode(port, role);

        System.out.println("Starting ServerNode at port " + port
                + " with role " + role);

        node.process();
    }
}
