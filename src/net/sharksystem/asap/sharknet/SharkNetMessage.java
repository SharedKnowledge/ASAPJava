package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.crypto.BasicCryptoParameters;
import net.sharksystem.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class SharkNetMessage {
    private static final int SIGNED_MASK = 0x1;
    private static final int ENCRYPTED_MASK = 0x2;
    private final byte[] snMessage;
    private final CharSequence snSender;
    private final boolean verified;
    private final boolean encrypted;
    private final CharSequence topic;

    public SharkNetMessage(byte[] snMessage, String snSender, boolean verified, boolean encrypted) {
        this(snMessage, null, snSender, verified, encrypted);
    }

    public SharkNetMessage(byte[] message, CharSequence topic, CharSequence sender, boolean verified, boolean encrypted) {
        this.snMessage = message;
        this.snSender = sender;
        this.verified = verified;
        this.encrypted = encrypted;
        this.topic = topic;
    }

    static byte[] serializeMessage(byte[] message, CharSequence topic, CharSequence recipient,
                 boolean sign, boolean encrypt, CharSequence ownerID, BasicCryptoParameters basicCryptoParameters)
            throws IOException, ASAPException {

        // merge content, sender and recipient
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeByteArray(message, baos);
        ASAPSerialization.writeCharSequenceParameter(ownerID, baos);
        ASAPSerialization.writeCharSequenceParameter(recipient, baos);
        message = baos.toByteArray();

        byte flags = 0;
        if(sign) {
            byte[] signature = ASAPCryptoAlgorithms.sign(message, basicCryptoParameters);
            baos = new ByteArrayOutputStream();
            ASAPSerialization.writeByteArray(message, baos); // message has three parts: content, sender, receiver
            // append signature
            ASAPSerialization.writeByteArray(signature, baos);
            // attach signature to message
            message = baos.toByteArray();
            flags += SIGNED_MASK;
        }

        if(encrypt) {
            message = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(
                    message, recipient, basicCryptoParameters);
            flags += ENCRYPTED_MASK;
        }

        // serialize SN message
        baos = new ByteArrayOutputStream();
        ASAPSerialization.writeByteParameter(flags, baos);
        ASAPSerialization.writeByteArray(message, baos);

        return baos.toByteArray();
    }

    static SharkNetMessage parseMessage(byte[] message, String sender, String uri,
                CharSequence ownerID, BasicCryptoParameters basicCryptoParameters) throws IOException, ASAPException {

        ByteArrayInputStream bais = new ByteArrayInputStream(message);
        byte flags = ASAPSerialization.readByte(bais);
        byte[] tmpMessage = ASAPSerialization.readByteArray(bais);

        boolean signed = (flags & SIGNED_MASK) != 0;
        boolean encrypted = (flags & ENCRYPTED_MASK) != 0;

        if(encrypted) {
            // decrypt
            bais = new ByteArrayInputStream(tmpMessage);
            ASAPCryptoAlgorithms.EncryptedMessagePackage
                    encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(bais);

            // for me?
            if(!encryptedMessagePackage.getRecipient().equals(ownerID)) {
                throw new ASAPException("SharkNetMessage: message not for me");
            }

            // replace message with decrypted message
            tmpMessage = ASAPCryptoAlgorithms.decryptPackage(
                    encryptedMessagePackage, basicCryptoParameters);
        }

        byte[] signature = null;
        byte[] signedMessage = null;
        if(signed) {
            // split message from signature
            bais = new ByteArrayInputStream(tmpMessage);
            tmpMessage = ASAPSerialization.readByteArray(bais);
            signedMessage = tmpMessage;
            signature = ASAPSerialization.readByteArray(bais);
        }
        bais = new ByteArrayInputStream(tmpMessage);
        byte[] snMessage = ASAPSerialization.readByteArray(bais);
        String snSender = ASAPSerialization.readCharSequenceParameter(bais);
        String snReceiver = ASAPSerialization.readCharSequenceParameter(bais);

        boolean verified = false; // initialize
        if(signature != null) {
            try {
                verified = ASAPCryptoAlgorithms.verify(
                        signedMessage, signature, snSender, basicCryptoParameters);
            }
            catch(ASAPSecurityException e) {
                // verified definitely false
                verified = false;
            }
        }

        return new SharkNetMessage(snMessage, snSender, verified, encrypted);
    }

    public byte[] getContent() { return this.snMessage;}

    public CharSequence getSender() { return this.snSender; }
    public boolean verified() { return this.verified; }
    public boolean encrypted() { return this.encrypted; }
}
