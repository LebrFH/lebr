package lebr;

public class Timer {

    private final String text;
    private long start;

    public Timer(final String text){
        this.text = text;
    }

    public static Timer start(final String text){
        final Timer timer = new Timer(text);
        timer.start();
        return timer;
    }

    public void start(){
        start = System.currentTimeMillis();
        System.out.println("Timer \"" + text + "\" gestartet.");
    }

    public void stop(){
        final long stop = System.currentTimeMillis();
        final long duration = stop - start;
        System.out.println("Timer \"" + text + "\" gestoppt. Dauer: " + duration + "ms.");
    }
}
