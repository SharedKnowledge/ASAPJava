package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

public class TCPHubConnector implements HubConnector {
    private final String hostName;
    private final int port;
    private NewConnectionListener listener;
    private Socket hubSocket;
    private ReaderThread readerThread;
    private Collection<CharSequence> peerIDs = new ArrayList<>();

    public TCPHubConnector() {
        this("localhost", Hub.DEFAULT_PORT);
    }

    public TCPHubConnector(CharSequence hostName) {
        this(hostName, Hub.DEFAULT_PORT);
    }

    public TCPHubConnector(int port) {
        this("localhost", port);
    }

    public TCPHubConnector(CharSequence hostName, int port) {
        this.hostName = hostName.toString();
        this.port = port;
    }

    public void setListener(NewConnectionListener listener) {
        this.listener = listener;
    }

    public Collection<CharSequence> getPeerIDs() throws IOException {
        this.checkConnected();

        return this.peerIDs;
    }

    @Override
    public void syncHubInformation() throws IOException {
        this.checkConnected();

        new RequestInfoPDU().sendPDU(this.hubSocket.getOutputStream());
    }

    private void checkConnected() throws IOException {
        if(this.hubSocket == null) throw new IOException("no hub connection");
    }

    @Override
    public void connectPeer(CharSequence peerID) throws IOException {
        this.checkConnected();

        // create hello pdu
        ConnectPDU connectPDU = new ConnectPDU(peerID);

        // ask for connection
        connectPDU.sendPDU(this.hubSocket.getOutputStream());
    }

    @Override
    public void connectHub(CharSequence localPeerID) throws IOException {
        // create TCP connection to hub
        this.hubSocket = new Socket(this.hostName, this.port);

        // create hello pdu
        HelloPDU helloPDU = new HelloPDU(localPeerID);

        // introduce yourself to hub
        helloPDU.sendPDU(this.hubSocket.getOutputStream());

        // start listening
        this.readerThread = new ReaderThread();
        this.readerThread.start();
    }

    @Override
    public void disconnect() throws IOException {
        Socket s = this.hubSocket;
        this.hubSocket = null; // null it in any case
        s.close();
    }

    private class ReaderThread extends Thread {
        public void run() {
            try {
                while(true) {
                    HubPDU hubPDU = HubPDU.readPDU(TCPHubConnector.this.hubSocket.getInputStream());
                    Log.writeLog(this, "read pdu from hub");
                    if (hubPDU instanceof NewConnectionPDU) {
                        Log.writeLog(this, "new connection pdu from hub");
                        NewConnectionPDU newConnectionPDU = (NewConnectionPDU) hubPDU;
                        if(TCPHubConnector.this.listener != null) {
                            // create a connection
                            Socket socket = new Socket(TCPHubConnector.this.hostName, newConnectionPDU.port);

                            // tell listener
                            TCPHubConnector.this.listener.notifyPeerConnected(
                                    new PeerConnection(
                                            newConnectionPDU.peerID,
                                            socket.getInputStream(),
                                            socket.getOutputStream()));
                        }
                    } else if (hubPDU instanceof ProvideHubInfoPDU) {
                        Log.writeLog(this, "provide hub information pdu from hub");
                        ProvideHubInfoPDU provideHubInfoPDU = (ProvideHubInfoPDU) hubPDU;
                        TCPHubConnector.this.peerIDs = provideHubInfoPDU.connectedPeers;
                    } else {
                        Log.writeLog(this, "unknown PDU, give up");
                        break;
                    }
                }
            } catch (IOException | ASAPException e) {
                Log.writeLog(this, "connection lost to hub");

            } catch (ClassCastException e) {
                Log.writeLog(this, "wrong pdu class - crazy: " + e.getLocalizedMessage());
            }
        }
    }
}
