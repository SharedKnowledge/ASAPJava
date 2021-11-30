package net.sharksystem.asap.apps.gossip;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.utils.Helper;
import net.sharksystem.asap.cmdline.TCPStream;
import net.sharksystem.asap.cmdline.TCPStreamCreatedListener;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

// java -cp ASAP_Engine_x.y.z.jar net.sharksystem.asap.apps.gossip.GossipUI Alice 7070 7071
public class GossipUI implements ASAPChunkReceivedListener {
    private List<Integer> remotePortNumber;
    private String peerName;
    private int portnumber;
    private ASAPInternalPeer asapInternalPeer;
    private String rootFolderName;

    public GossipUI(String peerName, int portnumber, List<Integer> portnumberlist) {
        this.peerName = peerName;
        this.portnumber = portnumber;
        this.remotePortNumber = portnumberlist;

        System.out.println("start peer " + this.peerName);
        System.out.println("local port " + this.portnumber);
        System.out.println("try to connect to ports " + this.remotePortNumber);
    }

    public static void main(String args[]) throws InterruptedException {
        if(args.length < 3) {
            System.err.println("Peer name, local port and least one port required to establish a network");
            System.exit(0);
        }

        // parse and set up
        String peerName = args[0];
        int portnumber = Integer.parseInt(args[1]);
        List<Integer> portnumberlist = new ArrayList<>();
        for(int i = 2; i < args.length; i++) {
            portnumberlist.add(Integer.parseInt(args[i]));
        }

        GossipUI gossip = new GossipUI(peerName, portnumber, portnumberlist);

        gossip.go();

        // wait forever
        Thread.sleep(10000);
    }

    private void println(String msg) {
        System.out.println("///////////////////////////////////////////////////");
        System.out.println("Example: " + msg);
        System.out.println("///////////////////////////////////////////////////");
    }

    private void go() {
        try {
            this.println("set up system");
            this.rootFolderName = this.peerName;
            this.asapInternalPeer = ASAPInternalPeerFS.createASAPPeer(this.peerName,
                    this.rootFolderName, ASAPInternalPeer.DEFAULT_MAX_PROCESSING_TIME, this);

            this.println("create engine");
            ASAPEngine exampleApp = asapInternalPeer.createEngineByFormat("exampleApp");

            DateFormat df = DateFormat.getInstance();
            String myMessage = "message from " + this.peerName + " at " + df.format(new Date());

            // convert to bytes
            byte[] byteMessage = Helper.str2bytes(myMessage);
            this.println("add message");
            exampleApp.add("exampleChannel", byteMessage);

            this.println("open tcp server port");
            // try to establish connections
            new ASAPConnectionCreator(new TCPStream(this.portnumber, true,
                    "server port: " + this.peerName));

            this.println("try to connect others");
            for(int port : this.remotePortNumber) {
                new ASAPConnectionCreator(port);
            }

        } catch (ASAPException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void chunkReceived(String format, String senderE2E, String uri, int era,
                              List<ASAPHop> asapHop) throws IOException {

        ASAPMessages receivedMessages =
                Helper.getMessagesByChunkReceivedInfos(format, senderE2E, uri, this.rootFolderName, era);

        this.println("chunk received: " + format + " | " + senderE2E + " | " + uri);

        Iterator<byte[]> messages = receivedMessages.getMessages();
        while(messages.hasNext()) {
            byte[] msgBytes = messages.next();
            String receivedMessage = Helper.bytes2str(msgBytes);
            this.println("message received: " + receivedMessage);
        };
    }

    private class ASAPConnectionCreator extends Thread implements TCPStreamCreatedListener {
        private TCPStream tcpStream;

        ASAPConnectionCreator(int port) {
            this(new TCPStream(port, false, "connect to " + String.valueOf(port)));
        }

        ASAPConnectionCreator(TCPStream tcpStream) {
            this.tcpStream = tcpStream;
            this.tcpStream.setListener(ASAPConnectionCreator.this);
            this.start();
        }

        public void run() {
            try {
                this.tcpStream.start();
                this.tcpStream.waitForConnection(Long.MAX_VALUE);
            } catch (IOException e) {
                println("wait for connection killed. ");
            }
        }

        @Override
        public void streamCreated(TCPStream channel) {
            try {
                GossipUI.this.asapInternalPeer.handleConnection(channel.getInputStream(), channel.getOutputStream());
            } catch (Exception e) {
                GossipUI.this.println("exception caught: " + e.getLocalizedMessage());
            }
        }
    }
}
