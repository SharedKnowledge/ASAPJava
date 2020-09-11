package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.ASAPUndecryptableMessageHandler;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.crypto.BasicCryptoParameters;
import net.sharksystem.crypto.ASAPCommunicationCryptoSettings;
import net.sharksystem.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ASAP_Modem_Impl implements ASAP_1_0 {
    private final BasicCryptoParameters signAndEncryptionKeyStorage;
    private final ASAPUndecryptableMessageHandler unencryptableMessageHandler;

    public ASAP_Modem_Impl() {
        this(null, null);
    }

    public ASAP_Modem_Impl(ASAPUndecryptableMessageHandler unencryptableMessageHandler) {
        this(null, unencryptableMessageHandler);
    }

    public ASAP_Modem_Impl(BasicCryptoParameters signAndEncryptionKeyStorage) {
        this(signAndEncryptionKeyStorage, null);
    }

    public ASAP_Modem_Impl(BasicCryptoParameters signAndEncryptionKeyStorage,
                           ASAPUndecryptableMessageHandler unencryptableMessageHandler) {
        this.signAndEncryptionKeyStorage = signAndEncryptionKeyStorage;
        this.unencryptableMessageHandler = unencryptableMessageHandler;
    }

    // Character are transmitted as bytes: number of bytes (first byte), content following, 0 mean no content
    /*
    general structure (asap message)
    a) no encryption
    CMD | FLAGS | ... specifics

    b) encryption
    CMD | algorithm | recipient | encrypted message len | encrypted FLAGS | ... specifics
    */

    @Override
    public void offer(CharSequence peer, CharSequence format, CharSequence channel, int era,
                      OutputStream os, boolean signed) throws IOException, ASAPException {
        OfferPDU_Impl.sendPDU(peer, format, channel, era, os, signed);
    }

    @Override
    public void offer(CharSequence recipient, CharSequence format, CharSequence channel,
                      OutputStream os, boolean signed) throws IOException, ASAPException {
        this.offer(recipient, format, channel, ERA_NOT_DEFINED, os, signed);
    }

    @Override
    public void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                         CharSequence channel, OutputStream os, boolean signed,
                         boolean encryted) throws IOException, ASAPException {

        this.interest(sender, recipient, format, channel, ERA_NOT_DEFINED, ERA_NOT_DEFINED, os,
                signed, encryted);
    }

    @Override
    public void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                         CharSequence channel, OutputStream os) throws IOException, ASAPException {

        this.interest(sender, recipient, format, channel, os, false, false);
    }

    @Override
    public void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                         CharSequence channel, int eraFrom, int eraTo, OutputStream os, boolean signed)
            throws IOException, ASAPException {

        this.interest(sender, recipient, format, channel, eraFrom, eraTo, os,
                signed, false);
    }

    @Override
    public void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                         CharSequence channel, int eraFrom, int eraTo, OutputStream os,
                         ASAPCommunicationCryptoSettings cryptoSettings)
            throws IOException, ASAPException {

        this.interest(sender, recipient, format, channel, eraFrom, eraTo, os,
                cryptoSettings.mustSign(), cryptoSettings.mustEncrypt());
    }


    @Override
    public void interest(CharSequence sender, CharSequence recipient, CharSequence format,
            CharSequence channel, int eraFrom, int eraTo, OutputStream os, boolean signed,
                         boolean encrypted) throws IOException, ASAPException {

        // prepare encryption and signing if required
        ASAPCryptoMessage cryptoMessage = new ASAPCryptoMessage(ASAP_1_0.INTEREST_CMD,
                os, signed, encrypted, recipient,
                this.signAndEncryptionKeyStorage);

        cryptoMessage.sendCmd();

        InterestPDU_Impl.sendPDUWithoutCmd(sender, recipient, format, channel, eraFrom, eraTo,
                cryptoMessage.getOutputStream(), signed);

        // finish crypto session - maybe nothing has to be done
        cryptoMessage.finish();
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, long length, List<Long> offsets, InputStream dataIS,
                           OutputStream os, boolean signed) throws IOException, ASAPException {

        this.assimilate(sender, recipient, format, channel, era, length, offsets, dataIS, os, signed, false);
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, long length, List<Long> offsets, InputStream dataIS,
                           OutputStream os, ASAPCommunicationCryptoSettings secureSetting) throws IOException, ASAPException {

        this.assimilate(sender, recipient, format, channel, era, length, offsets, dataIS, os,
                secureSetting.mustSign(), secureSetting.mustEncrypt());
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, long length, List<Long> offsets, InputStream dataIS,
                           OutputStream os, boolean signed, boolean encrypted) throws IOException, ASAPException {

        // prepare encryption and signing if required
        ASAPCryptoMessage cryptoMessage = new ASAPCryptoMessage(ASAP_1_0.ASSIMILATE_CMD,
                os, signed, encrypted, recipient,
                this.signAndEncryptionKeyStorage);

        cryptoMessage.sendCmd();

        AssimilationPDU_Impl.sendPDUWithoutCmd(sender, recipient, format, channel, era,
                length, offsets, dataIS, cryptoMessage.getOutputStream(), signed);

        // finish crypto session - maybe nothing has to be done
        cryptoMessage.finish();
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, List<Long> offsets, byte[] data,
                           OutputStream os, boolean signed) throws IOException, ASAPException {

        this.assimilate(sender, recipient, format, channel, era, offsets, data, os, signed, false);
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, List<Long> offsets, byte[] data,
                           OutputStream os, boolean signed, boolean encrypted) throws IOException, ASAPException {

        if(data == null || data.length == 0) throw new ASAPException("data must not be null");
        if(era < 0) throw new ASAPException("era must be a non-negative value: " + era);

        this.assimilate(sender, recipient, format, channel, era, data.length, offsets,
                new ByteArrayInputStream(data), os, signed, encrypted);
    }

    @Override
    public ASAP_PDU_1_0 readPDU(InputStream is) throws IOException, ASAPException {
        byte cmd = ASAPSerialization.readByte(is);

        // encrypted?
        boolean encrypted = (cmd & ENCRYPTED_MASK) != 0;

        if(encrypted) {
            ASAPCryptoMessage cryptoMessage = new ASAPCryptoMessage(this.signAndEncryptionKeyStorage);
            boolean ownerIsRecipient = cryptoMessage.initDecryption(cmd, is);
            if(ownerIsRecipient) {
                // peer is recipient - decrypt and go ahead
                InputStream decryptedIS = cryptoMessage.doDecryption();
                is = decryptedIS;
            } else {
                // we cannot decrypt this message - we are not recipient - but we keep and redistribute
                ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedASAPMessage = cryptoMessage.getEncryptedMessage();
                if(this.unencryptableMessageHandler != null) {
                    System.out.println(this.getLogStart() + "call handler to handle unencryptable message");
                    this.unencryptableMessageHandler.handleUndecryptableMessage(
                            encryptedASAPMessage, cryptoMessage.getReceiver());
                } else {
                    System.out.println(this.getLogStart() + "no handler for unencryptable messages found");
                }
                // throw exception anyway - could not create PDU
                throw new ASAPSecurityException("encryptable message received");
            }
        }

        int flagsInt = ASAPSerialization.readByte(is);

        InputStream realIS = is;
        ASAPCryptoMessage verifyCryptoMessage = null;
        if(PDU_Impl.flagSet(PDU_Impl.SIGNED_TO_BIT_POSITION, flagsInt)) {
            verifyCryptoMessage = new ASAPCryptoMessage(this.signAndEncryptionKeyStorage);
            is = verifyCryptoMessage.setupCopyInputStream(flagsInt, is);
        }

        PDU_Impl pdu = null;

        // remove encrypted flag
        cmd = (byte)(cmd & CMD_MASK);
        switch(cmd) {
            case ASAP_1_0.OFFER_CMD: pdu = new OfferPDU_Impl(flagsInt, encrypted, is); break;
            case ASAP_1_0.INTEREST_CMD: pdu = new InterestPDU_Impl(flagsInt, encrypted, is); break;
            case ASAP_1_0.ASSIMILATE_CMD: pdu = new AssimilationPDU_Impl(flagsInt, encrypted, is); break;
            default: throw new ASAPException("unknown command: " + cmd);
        }

        if(verifyCryptoMessage != null) {
            String sender = pdu.getSender();
            if(sender != null) {
                // read signature and try to verify
                try {
                    pdu.setVerified(verifyCryptoMessage.verify(sender, realIS));
                }
                catch(ASAPException e) {
                    System.out.println(this.getLogStart() + " cannot verify message");
                }
            }
        }

        return pdu;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
