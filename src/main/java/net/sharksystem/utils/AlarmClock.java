package net.sharksystem.utils;

public class AlarmClock extends Thread {
    public static final int DEFAULT_KEY = 42;
    private final long duration;
    private final int key;
    private final AlarmClockListener listener;
    private Thread thread;
    private boolean killed = false;

    public AlarmClock(long duration, int key, AlarmClockListener listener) {
        this.duration = duration;
        this.key = key;
        this.listener = listener;
    }

    public AlarmClock(long duration, AlarmClockListener listener) {
        this(duration, DEFAULT_KEY, listener);
    }

    public void kill() {
        this.killed = true;
    }

    public void run() {
        try {
            // killed before started ?
            if(!this.killed) {
                Thread.sleep(duration);
            }

            // killed during sleep?
            if(!this.killed) {
                this.listener.alarmClockRinging(this.key);
            }
        } catch (InterruptedException e) {
            // interrupted - nothing to do
        }
    }

    public String toString() {
        return "AlarmClock : " + this.key;
    }
}
