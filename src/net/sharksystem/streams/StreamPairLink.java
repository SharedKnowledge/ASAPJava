package net.sharksystem.streams;

import net.sharksystem.utils.Log;

import java.io.IOException;

public class StreamPairLink implements StreamPairListener {
    private StreamLink streamLinkA2B;
    private StreamLink streamLinkB2A;

    public StreamPairLink(StreamPair pairA, CharSequence idA, StreamPair pairB, CharSequence idB) throws IOException {
        this(pairA, idA, pairB, idB, true);
    }

    public StreamPairLink(
            StreamPair pairA, CharSequence idA,
            StreamPair pairB, CharSequence idB,
            boolean autostart) throws IOException {
        String tagA2B = idB + " ==> " + idA;
        this.streamLinkA2B = new StreamLink(pairA.getInputStream(), pairB.getOutputStream(), true, tagA2B);
        String tagB2A = idA + " ==> " + idB;
        this.streamLinkB2A = new StreamLink(pairB.getInputStream(), pairA.getOutputStream(), true, tagB2A);

        // listen to close
        pairA.addListener(this);
        pairB.addListener(this);

        if(autostart) this.start();
    }

    @Override
    public void notifyClosed(StreamPair closedStreamPair, String key) {
        Log.writeLog(this, "stream pair closed: " + key);
        this.streamLinkA2B.close();
        this.streamLinkB2A.close();
    }

    public void start() {
        this.streamLinkA2B.start();
        this.streamLinkB2A.start();
    }
}
