package net.sharksystem.asap.apps.mock;

import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.apps.ASAPSimplePeer;
import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;
import net.sharksystem.asap.util.Helper;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

public class ASAPSimplePeerFS extends ASAPBasicAbstractPeer implements ASAPSimplePeer,
        ASAPChunkReceivedListener, ASAPOnlinePeersChangedListener {
    private final ASAPPeer peer;
    private final String folderName;
    private ServerSocket serverSocket = null;
    private Socket socket = null;

    public ASAPSimplePeerFS(CharSequence peerName) throws IOException, ASAPException {
        super(peerName);
        this.folderName = "./peers/" + peerName;
        this.peer = ASAPPeerFS.createASAPPeer(peerName, folderName, this);
        this.peer.addOnlinePeersChangedListener(this);
    }

    @Override
    public void sendASAPMessage(CharSequence appName, CharSequence uri, byte[] message) throws ASAPException {
        try {
            ASAPEngine asapEngine = this.peer.createEngineByFormat(appName);
            asapEngine.activateOnlineMessages(this.peer);
            asapEngine.add(uri, message);
        } catch (IOException e) {
            this.log(e.getLocalizedMessage());
            throw new ASAPException("problems getting asap engine", e);
        }
    }

    public void startEncounter(int port, ASAPSimplePeerFS otherPeer) throws IOException {
        this.serverSocket = new ServerSocket(port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPSimplePeerFS.this.socket = ASAPSimplePeerFS.this.serverSocket.accept();
                } catch (IOException e) {
                    ASAPSimplePeerFS.this.log("fatal while waiting for client to connect: "
                            + e.getLocalizedMessage());
                }

                ASAPSimplePeerFS.this.startSession();
            }
        }).start();

        // wait a moment
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        otherPeer.connect(port);
    }

    private void connect(int port) throws IOException {
        this.socket = new Socket("localhost", port);
        this.startSession();
    }

    public void stopEncounter(ASAPSimplePeerFS otherPeer) throws IOException {
        this.socket.close();
    }

    private void startSession() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPSimplePeerFS.this.peer.handleConnection(
                            ASAPSimplePeerFS.this.socket.getInputStream(),
                            ASAPSimplePeerFS.this.socket.getOutputStream());
                } catch (IOException | ASAPException e) {
                    ASAPSimplePeerFS.this.log("fatal while connecting: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    @Override
    public CharSequence getPeerName() {
        return this.peer.getOwner();
    }

    @Override
    public void chunkReceived(String format, String sender, String uri, int era) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n++++++++++++++++++++++++++++++++ chunkReceived +++++++++++++++++++++++++++++++++\n");
        sb.append("app / format: " + format + " | " + sender + " | uri: " + uri + " | era: " + era);
        sb.append("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        this.log(sb.toString());

        ASAPMessages receivedMessages =
                Helper.getMessagesByChunkReceivedInfos(format, sender, uri, folderName, era);

        this.asapMessageReceivedListenerManager.notifyReceived(format, receivedMessages, true);
    }

    @Override
    public void onlinePeersChanged(ASAPPeer engine) {
        this.environmentChangesListenerManager.notifyListeners(engine.getOnlinePeers());
    }
}
