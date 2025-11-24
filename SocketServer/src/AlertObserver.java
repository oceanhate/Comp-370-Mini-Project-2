/**
 * Concrete Observer that sends alerts for critical events.
 * Demonstrates selective event handling - only reacts to critical events
 * like server failures and failovers.
 */
public class AlertObserver implements Observer {
    
    @Override
    public void update(String event) {
        // Only alert on critical events
        if (event.contains("DEAD") || event.contains("FAILED") || event.contains("FAILOVER")) {
            System.err.println("[ALERT!] CRITICAL EVENT: " + event);
        }
    }
}
