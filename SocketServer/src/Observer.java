/**
 * Observer interface for the Observer design pattern.
 * Classes implementing this interface can subscribe to Monitor events
 * and receive notifications when system state changes occur.
 */
public interface Observer {
    /**
     * Called when an event occurs in the Monitor.
     * @param event A string describing the event that occurred
     */
    void update(String event);
}
