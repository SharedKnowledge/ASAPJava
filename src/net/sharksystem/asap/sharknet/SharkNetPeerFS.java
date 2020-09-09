package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.*;
import net.sharksystem.asap.util.Helper;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.crypto.BasicCryptoParameters;
import net.sharksystem.utils.Serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private static final int SIGNED_MASK = 0x1;
    private static final int ENCRYPTED_MASK = 0x2;

    @Override
    public void sendSharkNetMessage(byte[] message, CharSequence topic, CharSequence recipient,
                                    boolean sign, boolean encrypt) throws IOException, ASAPException {

        // serialize sn message

        // merge content, sender and recipient
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serialization.writeByteArray(message, baos);
        Serialization.writeCharSequenceParameter(this.getOwnerID(), baos);
        Serialization.writeCharSequenceParameter(recipient, baos);
        message = baos.toByteArray();

        byte flags = 0;
        if(sign) {
            byte[] signature = ASAPCryptoAlgorithms.sign(message, this.basicCryptoParameters);
            baos = new ByteArrayOutputStream();
            Serialization.writeByteArray(message, baos);
            Serialization.writeByteArray(signature, baos);
            // attach signature to message
            message = baos.toByteArray();
            flags += SIGNED_MASK;
        }

        if(encrypt) {
            message = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(
                    message, recipient, this.basicCryptoParameters);
            flags += ENCRYPTED_MASK;
        }

        // serialize SN message
        baos = new ByteArrayOutputStream();
        Serialization.writeByteParameter(flags, baos);
        Serialization.writeByteArray(message, baos);

        this.sharkNetEngine.add(topic, baos.toByteArray());
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
                ByteArrayInputStream bais = new ByteArrayInputStream(message);
                try {
                    byte flags = Serialization.readByte(bais);
                    byte[] snMessage = Serialization.readByteArray(bais);

                    boolean signed = (flags & SIGNED_MASK) != 0;
                    boolean encrypted = (flags & ENCRYPTED_MASK) != 0;

                    if(encrypted) {
                        // decrypt
                        bais = new ByteArrayInputStream(snMessage);
                        ASAPCryptoAlgorithms.EncryptedMessagePackage
                                encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(bais);

                        // for me?
                        if(!encryptedMessagePackage.getRecipient().equals(this.getOwnerID())) {
                            System.out.println(this.getLogStart() + "message not for me");
                            continue; // still in asapStorage and will be redistributed
                        }

                        // replace message with decrypted message
                        snMessage = ASAPCryptoAlgorithms.decryptPackage(
                                encryptedMessagePackage, this.basicCryptoParameters);
                    }

                    byte[] signature = null;
                    if(signed) {
                        // split message from signature
                        bais = new ByteArrayInputStream(snMessage);
                        snMessage = Serialization.readByteArray(bais);
                        signature = Serialization.readByteArray(bais);
                    }
                    bais = new ByteArrayInputStream(snMessage);
                    String snSender = Serialization.readCharSequenceParameter(bais);
                    String snReceiver = Serialization.readCharSequenceParameter(bais);

                    boolean verified = false; // initialize
                    if(signature != null) {
                        verified = ASAPCryptoAlgorithms.verify(
                                snMessage, signature, snSender, this.basicCryptoParameters);
                    }

                    // we have anything - tell listeners
                    for(SharkNetMessageListener l : this.snListenerSet) {
                        l.messageReceived(snMessage, uri, snSender, verified, encrypted);
                    }
                } catch (ASAPException e) {
                    System.out.println(this.getLogStart() + "problems when deserializing SharkNet message");
                    continue; // try next
                }
            }
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
