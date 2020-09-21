package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.crypto.BasicKeyStore;
import net.sharksystem.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A SharkNet message is issued by a peer (sender), has content and can be tagged with an URI. It can have
 * null (to anybody), one or more recipients. A message can be signed. A message that is for a single recipient
 * can be encrypted.
 */
public class InMemoSNMessage implements SNMessage {
    private ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage;
    private byte[] snContent;
    private CharSequence snSender;
    private boolean verified;
    private boolean encrypted;
    private Set<CharSequence> snRecipients;
    private Timestamp creationTime;

    /**
     * Received
     * @param message
     * @param sender
     * @param verified
     * @param encrypted
     */
    private InMemoSNMessage(byte[] message, CharSequence sender,
                            Set<CharSequence> snRecipients, Timestamp creationTime,
                            boolean verified, boolean encrypted) {
        this.snContent = message;
        this.snSender = sender;
        this.verified = verified;
        this.encrypted = encrypted;
        this.snRecipients = snRecipients;
        this.creationTime = creationTime;
    }

    private InMemoSNMessage(ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage) {
        this.encryptedMessagePackage = encryptedMessagePackage;
        this.snRecipients = new HashSet<>();
        this.snRecipients.add(encryptedMessagePackage.getRecipient());
    }

    public static byte[] serializeMessage(byte[] content, CharSequence sender, CharSequence recipient)
            throws IOException, ASAPException {

        Set<CharSequence> recipients = null;
        if(recipient != null) {
            recipients = new HashSet<>();
            recipients.add(recipient);
        }

        return InMemoSNMessage.serializeMessage(content, sender, recipients,
                false, false, null);
    }

    public static byte[] serializeMessage(byte[] content, CharSequence sender, Set<CharSequence> recipients)
            throws IOException, ASAPException {

        return InMemoSNMessage.serializeMessage(content, sender, recipients,
                false, false, null);
    }

    public static byte[] serializeMessage(byte[] content, CharSequence sender, CharSequence recipient,
                                   boolean sign, boolean encrypt,
                                   BasicKeyStore basicKeyStore)
            throws IOException, ASAPException {

        Set<CharSequence> recipients = null;
        if(recipient != null) {
            recipients = new HashSet<>();
            recipients.add(recipient);
        }

        return InMemoSNMessage.serializeMessage(content, sender, recipients,
                sign, encrypt, basicKeyStore);

    }

    public static byte[] serializeMessage(byte[] content, CharSequence sender, Set<CharSequence> recipients,
        boolean sign, boolean encrypt, BasicKeyStore basicKeyStore)
            throws IOException, ASAPException {

        if( (recipients != null && recipients.size() > 1) && encrypt) {
            throw new ASAPSecurityException("cannot (yet) encrypt one message for more than one recipient - split it into more messages");
        }

        if(recipients == null) {
            recipients = new HashSet<>();
            recipients.add(SNMessage.ANY_RECIPIENT);
        }

        if(sender == null) {
            sender = SNMessage.ANONYMOUS;
        }

        /////////// produce serialized structure

        // merge content, sender and recipient
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ///// content
        ASAPSerialization.writeByteArray(content, baos);
        ///// sender
        ASAPSerialization.writeCharSequenceParameter(sender, baos);
        ///// recipients
        ASAPSerialization.writeCharSequenceSetParameter(recipients, baos);
        ///// timestamp
        Timestamp creationTime = new Timestamp(System.currentTimeMillis());
        String timestampString = creationTime.toString();
        ASAPSerialization.writeCharSequenceParameter(timestampString, baos);

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

    @Override
    public byte[] getContent() throws ASAPSecurityException {
        if(this.encryptedMessagePackage != null) {
            throw new ASAPSecurityException("content could not be encrypted");
        }
        return this.snContent;
    }

    @Override
    public CharSequence getSender() throws ASAPSecurityException {
        if(this.encryptedMessagePackage != null) {
            throw new ASAPSecurityException("content could not be encrypted");
        }

        return this.snSender;
    }

    @Override
    public Set<CharSequence> getRecipients() {
        return this.snRecipients;
    }

    @Override
    public boolean verified() throws ASAPSecurityException {
        if(this.encryptedMessagePackage != null) {
            throw new ASAPSecurityException("content could not be encrypted");
        }

        return this.verified;
    }

    @Override
    public boolean encrypted() {
        return this.encrypted;
    }

    public boolean couldBeDecrypted() {
        return this.encryptedMessagePackage != null;
    }

    @Override
    public Timestamp getCreationTime() throws ASAPSecurityException {
        if(this.encryptedMessagePackage != null) {
            throw new ASAPSecurityException("content could not be encrypted");
        }

        return this.creationTime;
    }

    @Override
    public boolean isLaterThan(SNMessage message) throws ASAPException, IOException {
        if(this.encryptedMessagePackage != null) {
            throw new ASAPSecurityException("content could not be encrypted");
        }

        Timestamp messageCreationTime = message.getCreationTime();
        return messageCreationTime.after(this.getCreationTime());
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //                                    factory methods                                   //
    //////////////////////////////////////////////////////////////////////////////////////////

    public static InMemoSNMessage parseMessage(byte[] message)
            throws IOException, ASAPException {

        return InMemoSNMessage.parseMessage(message, null);

    }

    public static InMemoSNMessage parseMessage(byte[] message, BasicKeyStore basicKeyStore)
            throws IOException, ASAPException {

        ByteArrayInputStream bais = new ByteArrayInputStream(message);
        byte flags = ASAPSerialization.readByte(bais);
        byte[] tmpMessage = ASAPSerialization.readByteArray(bais);

        boolean signed = (flags & SNMessage.SIGNED_MASK) != 0;
        boolean encrypted = (flags & SNMessage.ENCRYPTED_MASK) != 0;

        if (encrypted) {
            // decrypt
            bais = new ByteArrayInputStream(tmpMessage);
            ASAPCryptoAlgorithms.EncryptedMessagePackage
                    encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(bais);

            // for me?
            if (!basicKeyStore.isOwner(encryptedMessagePackage.getRecipient())) {
                return new InMemoSNMessage(encryptedMessagePackage);
                //throw new ASAPException("SharkNetMessage: message not for me");
            }

            // replace message with decrypted message
            tmpMessage = ASAPCryptoAlgorithms.decryptPackage(
                    encryptedMessagePackage, basicKeyStore);
        }

        byte[] signature = null;
        byte[] signedMessage = null;
        if (signed) {
            // split message from signature
            bais = new ByteArrayInputStream(tmpMessage);
            tmpMessage = ASAPSerialization.readByteArray(bais);
            signedMessage = tmpMessage;
            signature = ASAPSerialization.readByteArray(bais);
        }

        ///////////////// produce object form serialized bytes
        bais = new ByteArrayInputStream(tmpMessage);

        ////// content
        byte[] snMessage = ASAPSerialization.readByteArray(bais);
        ////// sender
        String snSender = ASAPSerialization.readCharSequenceParameter(bais);
        ////// recipients
        Set<CharSequence> snReceivers = ASAPSerialization.readCharSequenceSetParameter(bais);
        ///// timestamp
        String timestampString = ASAPSerialization.readCharSequenceParameter(bais);
        Timestamp creationTime = Timestamp.valueOf(timestampString);

        boolean verified = false; // initialize
        if (signature != null) {
            try {
                verified = ASAPCryptoAlgorithms.verify(
                        signedMessage, signature, snSender, basicKeyStore);
            } catch (ASAPSecurityException e) {
                // verified definitely false
                verified = false;
            }
        }

        return new InMemoSNMessage(snMessage, snSender, snReceivers, creationTime, verified, encrypted);
    }
}
