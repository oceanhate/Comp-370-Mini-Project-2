/**
 * Unified server class using Abstraction-Occurrence.
 * Represents either a PRIMARY or BACKUP server instance.
 */
public class ServerNode extends ServerProcess {

    public enum Role {
        PRIMARY,
        BACKUP
    }

    private final Role role;

    public ServerNode(int port, Role role) {
        super(port);
        this.role = role;
        this.isPrimary = (role == Role.PRIMARY);
    }

    @Override
    protected void onPromotedToPrimary() {
        if (role == Role.BACKUP) {
            System.out.println("*** Port " + this.serverPort + " WAS BACKUP, NOW I'M PRIMARY! ***");
        } else {
            System.out.println("I am Port " + this.serverPort + ". Confirmed as PRIMARY.");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Server <port> <PRIMARY|BACKUP>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        Role role = Role.valueOf(args[1].toUpperCase());

        new ServerNode(port, role).process();
    }
}
