/**
 * Concrete Observer that logs all Monitor events to the console.
 * This demonstrates the Observer pattern by decoupling logging logic
 * from the Monitor's core functionality.
 */
public class LoggingObserver implements Observer {
    
    @Override
    public void update(String event) {
        System.out.println("[LOG] " + event);
    }
}
