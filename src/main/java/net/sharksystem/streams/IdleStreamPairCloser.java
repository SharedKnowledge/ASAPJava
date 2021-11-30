package net.sharksystem.streams;

import net.sharksystem.utils.AlarmClock;
import net.sharksystem.utils.AlarmClockListener;

import java.io.IOException;

public class IdleStreamPairCloser implements AlarmClockListener, WrappedStreamPairListener {
    private final int timeout;
    private StreamPairWrapper streamPairWrapper;
    private AlarmClock alarmClock;

    private IdleStreamPairCloser(int timeout) {
        this.timeout = timeout;
    }

    public static IdleStreamPairCloser getIdleStreamsCloser(StreamPair streamPair, int timeout) throws IOException {
        // create object that also becomes listener
        IdleStreamPairCloser idleStreamPairCloser = new IdleStreamPairCloser(timeout);

        // create wrapper with listener
        StreamPairWrapper streamPairWrapper = new StreamPairWrapper(
                streamPair.getInputStream(), streamPair.getOutputStream(), idleStreamPairCloser, "X");

        // now: put wrapper into listener
        idleStreamPairCloser.setStreamPairWrapper(streamPairWrapper);
        return idleStreamPairCloser;
    }

    void setStreamPairWrapper(StreamPairWrapper streamPairWrapper) {
        this.streamPairWrapper = streamPairWrapper;
    }

    public void start() {
        // give it more time in the first round - there will be a connection establishment process on its way...
        this.alarmClock = new AlarmClock(this.timeout * 2, this);
    }

    @Override
    public void alarmClockRinging(int i) {
        try {
            this.streamPairWrapper.getInputStream().close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void notifyClosed(StreamPair streamPair, String s) {
        this.alarmClock.kill(); // nothing todo.
        this.alarmClock = null;
    }

    @Override
    public void notifyAction(String s) {
        if(alarmClock != null) {
            this.alarmClock.kill();
            this.alarmClock = new AlarmClock(timeout, this);
        } // else not yet started
    }
}
