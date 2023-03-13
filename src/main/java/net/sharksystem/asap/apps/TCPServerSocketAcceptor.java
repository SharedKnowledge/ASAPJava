package net.sharksystem.asap.apps;

import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.EncounterConnectionType;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.tcp.SocketFactory;
import net.sharksystem.utils.tcp.StreamPairCreatedListener;

import java.io.IOException;

public class TCPServerSocketAcceptor implements StreamPairCreatedListener {
    private final ASAPEncounterManager encounterManager;

    public TCPServerSocketAcceptor(int portNumber, ASAPEncounterManager encounterManager) throws IOException {
        this.encounterManager = encounterManager;
        SocketFactory socketFactory = new SocketFactory(portNumber, this);

        Log.writeLog(this, "start socket factory");
        new Thread(socketFactory).start();
    }

    @Override
    public void streamPairCreated(StreamPair streamPair) {
        Log.writeLog(this, "new stream pair created");
        try {
            this.encounterManager.handleEncounter(streamPair, EncounterConnectionType.INTERNET);
        } catch (IOException e) {
            Log.writeLogErr(this, "exception when asking for new connection handling: "
                    + e.getLocalizedMessage());
        }
    }
}
