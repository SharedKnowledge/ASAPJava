package net.sharksystem.asap.apps.testsupport;

import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.EncounterConnectionType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

public class ASAPTestPeerFS extends ASAPPeerFS {
    private ServerSocket serverSocket = null;
    private Socket socket = null;

    public ASAPTestPeerFS(CharSequence peerName, Collection<CharSequence> supportedFormats) throws IOException, ASAPException {
        this(peerName, "./testPeerFS/" + peerName, supportedFormats);
    }

    public ASAPTestPeerFS(CharSequence peerName, CharSequence rootFolder, Collection<CharSequence> supportedFormats)
            throws IOException, ASAPException {
        super(peerName, rootFolder, supportedFormats);
    }

    public void startEncounter(int port, ASAPTestPeerFS otherPeer) throws IOException {
        this.serverSocket = new ServerSocket(port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPTestPeerFS.this.socket = ASAPTestPeerFS.this.serverSocket.accept();
                } catch (IOException e) {
                    ASAPTestPeerFS.this.log("fatal while waiting for client to connect: "
                            + e.getLocalizedMessage());
                }

                ASAPTestPeerFS.this.startSession();
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

    public void stopEncounter(ASAPTestPeerFS otherPeer) throws IOException {
        this.socket.close();
    }

    private void startSession() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPTestPeerFS.this.handleConnection(
                            ASAPTestPeerFS.this.socket.getInputStream(),
                            ASAPTestPeerFS.this.socket.getOutputStream(),
                            EncounterConnectionType.INTERNET);
                } catch (IOException | ASAPException e) {
                    ASAPTestPeerFS.this.log("fatal while connecting: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }
}
