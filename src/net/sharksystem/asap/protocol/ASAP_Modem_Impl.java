package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ASAP_Modem_Impl implements ASAP_1_0 {
    private final ASAPReadonlyKeyStorage signAndEncryptionKeyStorage;

    public ASAP_Modem_Impl() {
        this.signAndEncryptionKeyStorage = null; // no key storage
    }

    public ASAP_Modem_Impl(ASAPReadonlyKeyStorage signAndEncryptionKeyStorage) {
        this.signAndEncryptionKeyStorage = signAndEncryptionKeyStorage;
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
            CharSequence channel, int eraFrom, int eraTo, OutputStream os, boolean signed,
                         boolean encrypted)
            throws IOException, ASAPException, ASAPSecurityException {

        // prepare encryption and signing if required
        CryptoSession cryptoSession = new CryptoSession(ASAP_1_0.INTEREST_CMD,
                os, signed, encrypted, recipient,
                this.signAndEncryptionKeyStorage);

        cryptoSession.sendCmd();

        InterestPDU_Impl.sendPDUWithoutCmd(sender, recipient, format, channel, eraFrom, eraTo,
                cryptoSession.getOutputStream(), signed);

        // finish crypto session - if any
        cryptoSession.finish();
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, long length, List<Long> offsets, InputStream dataIS,
                           OutputStream os, boolean signed) throws IOException, ASAPException {

        AssimilationPDU_Impl.sendPDU(sender, recipient, format, channel, era, length, offsets, dataIS, os, signed);
    }

    @Override
    public void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, List<Long> offsets, byte[] data,
                           OutputStream os, boolean signed) throws IOException, ASAPException {

        if(data == null || data.length == 0) throw new ASAPException("data must not be null");
        if(era < 0) throw new ASAPException("era must be a non-negative value: " + era);

        this.assimilate(sender, recipient, format, channel, era, data.length, offsets,
                new ByteArrayInputStream(data), os, signed);
    }

    @Override
    public ASAP_PDU_1_0 readPDU(InputStream is) throws IOException, ASAPException {
        byte cmd = PDU_Impl.readByte(is);

        // encrypted?
        boolean encrypted = (cmd & ENCRYPTED_MASK) != 0;
        // remove encrypted flag
        cmd = (byte)(cmd & CMD_MASK);

        if(encrypted) {
            try {
                CryptoSession cryptoSession = new CryptoSession(this.signAndEncryptionKeyStorage);
                InputStream decryptedIS = cryptoSession.decrypt(is);
                is = decryptedIS;
            }
            catch(ASAPSecurityException e) {
                System.out.println(this.getLogStart() + "cannot decrypt message. TODO: Store (according to some rules) and forward it?!");
            }
        }

        int flagsInt = PDU_Impl.readByte(is);

        InputStream realIS = is;
        CryptoSession verifyCryptoSession = null;
        if(PDU_Impl.flagSet(PDU_Impl.SIGNED_TO_BIT_POSITION, flagsInt)) {
            verifyCryptoSession = new CryptoSession(this.signAndEncryptionKeyStorage);
            is = verifyCryptoSession.setupInputStreamListener(is, flagsInt);
        }

        PDU_Impl pdu = null;

        switch(cmd) {
            case ASAP_1_0.OFFER_CMD: pdu = new OfferPDU_Impl(flagsInt, encrypted, is); break;
            case ASAP_1_0.INTEREST_CMD: pdu = new InterestPDU_Impl(flagsInt, encrypted, is); break;
            case ASAP_1_0.ASSIMILATE_CMD: pdu = new AssimilationPDU_Impl(flagsInt, encrypted, is); break;
            default: throw new ASAPException("unknown command: " + cmd);
        }

        if(verifyCryptoSession != null) {
            String sender = pdu.getSender();
            if(sender != null) {
                // read signature and try to verify
                try {
                    pdu.setVerified(verifyCryptoSession.verify(sender, realIS));
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
