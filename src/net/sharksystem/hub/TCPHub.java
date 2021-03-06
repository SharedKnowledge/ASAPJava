package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TCPHub extends Thread implements Hub {
    private static final int DEFAULT_MAX_IDLE_CONNECTION_IN_SECONDS = 60;
    private int maxIdleInSeconds = DEFAULT_MAX_IDLE_CONNECTION_IN_SECONDS;
    private final int port;
    private final ServerSocket serverSocket;
    private int minPort = 0;
    private int maxPort = 0;
    private int nextPort = 0;
    private Map<CharSequence, PeerConnection> connectors = new HashMap<>();

    public TCPHub() throws IOException {
        this(Hub.DEFAULT_PORT);
    }

    public TCPHub(int port) throws IOException {
        this.port = port;
        this.nextPort = port+1;
        this.serverSocket = new ServerSocket(this.port);
    }

    public void setPortRange(int minPort, int maxPort) throws ASAPException {
        if(minPort < -1 || maxPort < -1 || maxPort <= minPort) {
            throw new ASAPException("port number must be > 0 and max > min");
        }

        this.minPort = minPort;
        this.maxPort = maxPort;

        this.nextPort = this.minPort;
    }

    public void setMaxIdleConnectionInSeconds(int maxIdleInSeconds) {
        this.maxIdleInSeconds = maxIdleInSeconds;
    }

    private synchronized ServerSocket getServerSocket() throws IOException {
        if(this.minPort == 0 || this.maxPort == 0) {
            return new ServerSocket(0);
        }

        int port = this.nextPort++;
        // try
        while(port <= this.maxPort) {
            try {
                ServerSocket srv = new ServerSocket(port);
                return srv;
            } catch (IOException ioe) {
                // port already in use
            }
            port = this.nextPort++;
        }
        this.nextPort = this.minPort; // rewind for next round
        throw new IOException("all ports are in use");
    }

    @Override
    public void run() {
        try {
            while(true) {
                Socket hubConnector = this.serverSocket.accept();

                // read hello pdu
                HelloPDU helloPDU = (HelloPDU) HubPDU.readPDU(hubConnector.getInputStream());
                Log.writeLog(this, "new connector: " + helloPDU.peerID);

                  PeerConnection peerConnection = this.connectors.get(helloPDU.peerID);
                if (peerConnection != null) {
                    Log.writeLog(this, "already connected: " + helloPDU.peerID);
                    // already exists
                    hubConnector.close();
                } else {
                    // get already connected peers
                    Log.writeLog(this, "send hub info pdu" + helloPDU.peerID);
                    Set<CharSequence> connectedPeers = this.connectors.keySet();
                    HubPDU hubInfoPDU = new ProvideHubInfoPDU(connectedPeers);
                    hubInfoPDU.sendPDU(hubConnector.getOutputStream());

                    PeerConnection newConnectorStreams = new PeerConnection(
                            helloPDU.peerID, hubConnector.getInputStream(), hubConnector.getOutputStream());

                    this.connectors.put(helloPDU.peerID, newConnectorStreams);
                    Log.writeLog(this, "remember: " + helloPDU.peerID);

                    // start reading from connector
                    new ConnectorThread(newConnectorStreams).start();
                }
            }
        } catch (IOException | ASAPException e) {
            // gone
            Log.writeLog(this, "TCP Hub died: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void addASAPPeer(ASAPPeer peer) {
        // TODO
    }

    @Override
    public void removeASAPPeer(ASAPPeer peer) {
        // TODO
    }

    private class ConnectorThread extends Thread {
        private final PeerConnection peerConnection;

        ConnectorThread(PeerConnection peerConnection) {
            this.peerConnection = peerConnection;
        }

        @Override
        public void run() {
            try {
                Log.writeLog(this, "launch connector thread to : " + this.peerConnection.peerID);
                while(true) {
                    // read and most probably wait
                    HubPDU hubPDU = HubPDU.readPDU(this.peerConnection.is);
                    Log.writeLog(this, this.peerConnection.peerID + ": read pdu");

                    if(hubPDU instanceof ConnectPDU) {
                        Log.writeLog(this, this.peerConnection.peerID + ": read connect pdu");
                        ConnectPDU connectPDU = (ConnectPDU) hubPDU;
                        PeerConnection connectToPeerConnection = TCPHub.this.connectors.get(connectPDU.peerID);
                        if(connectToPeerConnection == null) {
                            Log.writeLog(this, "cannot connect to: " + connectPDU.peerID);
                            // maybe local peer - TODO
                        } else {
                            ServerSocket serverSocket1 = TCPHub.this.getServerSocket();
                            ServerSocket serverSocket2 = TCPHub.this.getServerSocket();
                            TwistedConnection twistedConnection =
                                    new TwistedConnection(serverSocket1, serverSocket2, maxIdleInSeconds);
                            twistedConnection.start();

                            // send to peer that asked for connection
                            NewConnectionPDU newConnectionPDU = new NewConnectionPDU(
                                    serverSocket1.getLocalPort(),
                                    connectPDU.peerID);

                            newConnectionPDU.sendPDU(this.peerConnection.os);

                            // send to peer that was asked to be connected
                            newConnectionPDU = new NewConnectionPDU(
                                    serverSocket2.getLocalPort(),
                                    this.peerConnection.peerID);

                            newConnectionPDU.sendPDU(connectToPeerConnection.os);
                        }
                    } else if(hubPDU instanceof RequestInfoPDU) {
                        Log.writeLog(this, this.peerConnection.peerID + ": read request info pdu");
                        Set<CharSequence> allPeers = TCPHub.this.connectors.keySet();
                        // sort out calling peer
                        Set<CharSequence> peersWithoutCaller = new HashSet();
                        for(CharSequence peerName : allPeers) {
                            if(!peerName.toString().equalsIgnoreCase(this.peerConnection.peerID.toString())) {
                                peersWithoutCaller.add(peerName);
                            }
                        }
                        HubPDU hubInfoPDU = new ProvideHubInfoPDU(peersWithoutCaller);
                        hubInfoPDU.sendPDU(this.peerConnection.os);
                    }
                }
            } catch (IOException | ASAPException e) {
                Log.writeLog(this, "connection lost to: " + this.peerConnection.peerID);
            } catch (ClassCastException e) {
                Log.writeLog(this, "wrong pdu class - crazy: " + e.getLocalizedMessage());
            }
            finally {
                TCPHub.this.connectors.remove(this.peerConnection.peerID);
            }
        }
    }
}
