package net.sharksystem.asap.apps.mock;

import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.ASAPPeerServices;
import net.sharksystem.asap.util.Helper;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ASAPSimplePeer extends ASAPBasicAbstractPeer implements ASAPPeerServices, ASAPChunkReceivedListener {
    private final ASAPPeer peer;
    private final String folderName;
    private ServerSocket serverSocket = null;
    private Socket socket = null;

    public ASAPSimplePeer(CharSequence peerName) throws IOException, ASAPException {
        super(peerName);
        this.folderName = "./peers/" + peerName;
        File asapFolder = new File(folderName);
        if(!asapFolder.exists()) {
            asapFolder.mkdirs();
        }
        this.peer = ASAPPeerFS.createASAPPeer(peerName, folderName, this);
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


    public void startEncounter(int port, ASAPSimplePeer otherPeer) throws IOException {
        this.serverSocket = new ServerSocket(port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPSimplePeer.this.socket = ASAPSimplePeer.this.serverSocket.accept();
                } catch (IOException e) {
                    ASAPSimplePeer.this.log("fatal while waiting for client to connect: "
                            + e.getLocalizedMessage());
                }

                ASAPSimplePeer.this.startSession();
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

    public void stopEncounter(ASAPSimplePeer otherPeer) throws IOException {
        this.socket.close();
    }

    private void startSession() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPSimplePeer.this.peer.handleConnection(
                            ASAPSimplePeer.this.socket.getInputStream(),
                            ASAPSimplePeer.this.socket.getOutputStream());
                } catch (IOException | ASAPException e) {
                    ASAPSimplePeer.this.log("fatal while connecting: " + e.getLocalizedMessage());
                }
            }
        }).start();
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
}
