package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.*;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.util.Helper;
import net.sharksystem.crypto.BasicCryptoParameters;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SharkNetPeerFS implements SharkNetPeer, ASAPChunkReceivedListener {
    private final ASAPPeer asapPeer;
    private final ASAPEngine sharkNetEngine;
    private final BasicCryptoParameters basicCryptoParameters;
    private final DefaultSecurityAdministrator securityAdministrator;
    private final String rootFolder;
    private final Set<SharkNetMessageListener> snListenerSet = new HashSet<>();

    public SharkNetPeerFS(String ownerID, String rootFolder, BasicCryptoParameters basicCryptoParameters)
            throws IOException, ASAPException {

        this.rootFolder = rootFolder;
        this.asapPeer = ASAPPeerFS.createASAPPeer(ownerID, rootFolder, this);

        this.asapPeer.setASAPBasicKeyStorage(basicCryptoParameters);
        this.securityAdministrator = new DefaultSecurityAdministrator();
        this.asapPeer.setSecurityAdministrator(this.securityAdministrator);

        this.sharkNetEngine = this.asapPeer.createEngineByFormat(SharkNet.SHARKNET_FORMAT);
        this.basicCryptoParameters = basicCryptoParameters;
    }

    @Override
    public CharSequence getOwnerID() {
        return this.asapPeer.getOwner();
    }

    @Override
    public void sendSharkNetMessage(byte[] message, CharSequence topic, CharSequence recipient,
                                    boolean sign, boolean encrypt) throws IOException, ASAPException {

        // serialize sn message
        this.sharkNetEngine.add(topic,
                SharkNetMessage.serializeMessage(message, topic, recipient, sign, encrypt,
                        this.getOwnerID(), this.basicCryptoParameters));
    }

    @Override
    public void addSharkNetMessageListener(SharkNetMessageListener snLister) {
        this.snListenerSet.add(snLister);
    }

    @Override
    public void removeSharkNetMessageListener(SharkNetMessageListener snLister) {
        this.snListenerSet.remove(snLister);
    }

    @Override
    public void chunkReceived(String format, String sender, String uri, int era) throws IOException {
        if(format.equalsIgnoreCase(SharkNet.SHARKNET_FORMAT)) {
            ASAPMessages receivedMessages = Helper.getMessagesByChunkReceivedInfos(
                    format, sender, uri,
                    rootFolder, // see peer creation
                    era);

            Iterator<byte[]> msgIter = receivedMessages.getMessages();
            while(msgIter.hasNext()) {
                byte[] message = msgIter.next();

                // deserialize SNMessage
                try {
                    SharkNetMessage snMessage =
                        SharkNetMessage.parseMessage(message, sender, uri, this.getOwnerID(), this.basicCryptoParameters);

                    // we have anything - tell listeners
                    for(SharkNetMessageListener l : this.snListenerSet) {
                        l.messageReceived(snMessage.getContent(), uri, snMessage.getSender(), snMessage.verified(),
                                snMessage.encrypted());
                    }
                } catch (ASAPException e) {
                    System.out.println(this.getLogStart() + "problems when deserializing SharkNet message");
                    e.printStackTrace();
                    continue; // try next
                }
            }
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    @Override
    public ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        return this.asapPeer.handleConnection(is, os);
    }
}
