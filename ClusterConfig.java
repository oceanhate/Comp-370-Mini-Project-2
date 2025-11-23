public class ClusterConfig {

    public static final String HOST = "localhost";

    public static class NodeInfo {
        public final int port;
        public final ServerNode.Role role;

        public NodeInfo(int port, ServerNode.Role role) {
            this.port = port;
            this.role = role;
        }
    }

    // These are the occurrences
    public static final NodeInfo[] NODES = {
            new NodeInfo(8090, ServerNode.Role.PRIMARY),
            new NodeInfo(8089, ServerNode.Role.BACKUP),
            new NodeInfo(8088, ServerNode.Role.BACKUP)
    };
}
