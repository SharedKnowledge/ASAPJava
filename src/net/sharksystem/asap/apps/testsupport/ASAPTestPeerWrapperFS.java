package net.sharksystem.asap.apps.testsupport;

import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

public class ASAPTestPeerWrapperFS extends ASAPPeerFS {
    private ServerSocket serverSocket = null;
    private Socket socket = null;

    public ASAPTestPeerWrapperFS(CharSequence peerName, Collection<CharSequence> supportedFormats) throws IOException, ASAPException {
        super(peerName, "./testPeerFS/" + peerName, supportedFormats);
    }

    public void startEncounter(int port, ASAPTestPeerWrapperFS otherPeer) throws IOException {
        this.serverSocket = new ServerSocket(port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPTestPeerWrapperFS.this.socket = ASAPTestPeerWrapperFS.this.serverSocket.accept();
                } catch (IOException e) {
                    ASAPTestPeerWrapperFS.this.log("fatal while waiting for client to connect: "
                            + e.getLocalizedMessage());
                }

                ASAPTestPeerWrapperFS.this.startSession();
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

    public void stopEncounter(ASAPTestPeerWrapperFS otherPeer) throws IOException {
        this.socket.close();
    }

    private void startSession() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ASAPTestPeerWrapperFS.this.handleConnection(
                            ASAPTestPeerWrapperFS.this.socket.getInputStream(),
                            ASAPTestPeerWrapperFS.this.socket.getOutputStream());
                } catch (IOException | ASAPException e) {
                    ASAPTestPeerWrapperFS.this.log("fatal while connecting: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

}
