package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.crypto.BasicKeyStore;
import net.sharksystem.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A SharkNet message is issued by a peer (sender), has content and can be tagged with an URI. It can have
 * null (to anybody), one or more recipients. A message can be signed. A message that is for a single recipient
 * can be encrypted.
 */
public class SharkNetMessage {
    private static final int SIGNED_MASK = 0x1;
    private static final int ENCRYPTED_MASK = 0x2;
    public static final CharSequence ANY_RECIPIENT = "SN_ANY";
    public static final CharSequence ANONYMOUS = "SN_ANON";
    private byte[] snContent;
    private final CharSequence snSender;
    private byte[] serializedMessage;
    private boolean verified;
    private boolean encrypted;
    private final CharSequence topic;
    private Set<CharSequence> snRecipients;

    SharkNetMessage(byte[] snContent, String snSender, boolean verified, boolean encrypted) {
        this(snContent, null, snSender, new HashSet<>(), verified, encrypted);
    }

    /**
     * Received
     * @param message
     * @param topic
     * @param sender
     * @param verified
     * @param encrypted
     */
    SharkNetMessage(byte[] message, CharSequence topic, CharSequence sender,
                    Set<CharSequence> snRecipients,
                    boolean verified, boolean encrypted) {
        this.snContent = message;
        this.snSender = sender;
        this.verified = verified;
        this.encrypted = encrypted;
        this.topic = topic;
        this.snRecipients = snRecipients;
    }

    /**
     * Use this constructor to set a message that is to be sent
     * @param content content
     * @param topic uri
     * @param sender sender - me - or a synonym
     * @param recipient message recipient
     * @param sign sing message
     * @param encrypt encrypt message
     */
    public SharkNetMessage(byte[] content, CharSequence topic, CharSequence sender, CharSequence recipient,
                           boolean sign, boolean encrypt, BasicKeyStore basicKeyStore)
                throws IOException, ASAPException {

        this.snContent = content;
        this.snSender = sender;
        this.topic = topic;

        this.serializedMessage =
                SharkNetMessage.serializeMessage(content, topic, recipient,
                        sign, encrypt, sender, basicKeyStore);
    }

    /**
     * Use this constructor to set a message that is to be sent
     * @param content
     * @param topic
     * @param sender
     * @param recipients more than one recipient - such a message cannot (yet) be signed. See group key project.
     * @param sign
     */
    public SharkNetMessage(byte[] content, CharSequence topic, CharSequence sender,
                           Set<CharSequence> recipients, boolean sign,
                           BasicKeyStore basicKeyStore) throws IOException, ASAPException {

        this.snSender = sender;
        this.topic = topic;

        this.serializedMessage =
                SharkNetMessage.serializeMessage(content, topic, recipients,
                        sign, false, sender, basicKeyStore);

    }

    static byte[] serializeMessage(byte[] message, CharSequence topic, CharSequence recipient,
                                   boolean sign, boolean encrypt, CharSequence ownerID,
                                   BasicKeyStore basicKeyStore)
            throws IOException, ASAPException {

        Set<CharSequence> recipients = null;
        if(recipient != null) {
            recipients = new HashSet<>();
            recipients.add(recipient);
        }

        return SharkNetMessage.serializeMessage(message, topic, recipients,
                sign, encrypt, ownerID, basicKeyStore);

    }

    static byte[] serializeMessage(byte[] content, CharSequence topic, Set<CharSequence> recipients,
        boolean sign, boolean encrypt, CharSequence sender, BasicKeyStore basicKeyStore)
            throws IOException, ASAPException {

        if( (recipients != null && recipients.size() > 1) && encrypt) {
            throw new ASAPSecurityException("cannot (yet) encrypt one message for more than one recipient - split it into more messages");
        }

        if(recipients == null) {
            recipients = new HashSet<>();
            recipients.add(SharkNetMessage.ANY_RECIPIENT);
        }

        if(sender == null) {
            sender = SharkNetMessage.ANONYMOUS;
        }

        // merge content, sender and recipient
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeByteArray(content, baos);
        ASAPSerialization.writeCharSequenceParameter(sender, baos);
//        ASAPSerialization.writeCharSequenceParameter(recipient, baos);
        ASAPSerialization.writeCharSequenceSetParameter(recipients, baos);
        content = baos.toByteArray();

        byte flags = 0;
        if(sign) {
            byte[] signature = ASAPCryptoAlgorithms.sign(content, basicKeyStore);
            baos = new ByteArrayOutputStream();
            ASAPSerialization.writeByteArray(content, baos); // message has three parts: content, sender, receiver
            // append signature
            ASAPSerialization.writeByteArray(signature, baos);
            // attach signature to message
            content = baos.toByteArray();
            flags += SIGNED_MASK;
        }

        if(encrypt) {
            content = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(
                    content,
                    recipients.iterator().next(), // already checked if one and only one is recipient
                    basicKeyStore);
            flags += ENCRYPTED_MASK;
        }

        // serialize SN message
        baos = new ByteArrayOutputStream();
        ASAPSerialization.writeByteParameter(flags, baos);
        ASAPSerialization.writeByteArray(content, baos);

        return baos.toByteArray();
    }

    public static SharkNetMessage parseMessage(byte[] message, String uri,
                CharSequence ownerID, BasicKeyStore basicKeyStore) throws IOException, ASAPException {

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
                    encryptedMessagePackage, basicKeyStore);
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
        Set<CharSequence> snReceivers = ASAPSerialization.readCharSequenceSetParameter(bais);
        //String snReceiver = ASAPSerialization.readCharSequenceParameter(bais);

        boolean verified = false; // initialize
        if(signature != null) {
            try {
                verified = ASAPCryptoAlgorithms.verify(
                        signedMessage, signature, snSender, basicKeyStore);
            }
            catch(ASAPSecurityException e) {
                // verified definitely false
                verified = false;
            }
        }

        return new SharkNetMessage(snMessage, uri, snSender, snReceivers, verified, encrypted);
    }

    public byte[] getContent() { return this.snContent;}
    public CharSequence getSender() { return this.snSender; }
    public Set<CharSequence> getRecipients() { return this.snRecipients; }
    public boolean verified() { return this.verified; }
    public boolean encrypted() { return this.encrypted; }

    public byte[] getSerializedMessage() {
        return this.serializedMessage;
    }
}
