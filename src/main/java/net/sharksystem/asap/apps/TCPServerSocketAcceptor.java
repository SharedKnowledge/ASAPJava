package net.sharksystem.asap.apps;

import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.tcp.SocketFactory;
import net.sharksystem.utils.tcp.StreamPairCreatedListener;

import java.io.IOException;

public class TCPServerSocketAcceptor implements StreamPairCreatedListener {
    private final ASAPEncounterManager encounterManager;
    private final SocketFactory socketFactory;

    public TCPServerSocketAcceptor(int portNumber, ASAPEncounterManager encounterManager, boolean remainOpen)
            throws IOException {
        this.encounterManager = encounterManager;
        this.socketFactory = new SocketFactory(portNumber, this, remainOpen);

        Log.writeLog(this, "start socket factory - no race condition assumed");
        new Thread(socketFactory).start();
    }

    public TCPServerSocketAcceptor(int portNumber, ASAPEncounterManager encounterManager) throws IOException {
        this(portNumber, encounterManager, false);
    }

    public void close() throws IOException {
        this.socketFactory.close();
    }

    @Override
    public void streamPairCreated(StreamPair streamPair) {
        Log.writeLog(this, "new stream pair created");
        try {
            this.encounterManager.handleEncounter(streamPair, ASAPEncounterConnectionType.INTERNET);
        } catch (IOException e) {
            Log.writeLogErr(this, "exception when asking for new connection handling: "
                    + e.getLocalizedMessage());
        }
    }
}
