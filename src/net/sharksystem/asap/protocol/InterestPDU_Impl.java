package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class InterestPDU_Impl extends PDU_Impl implements ASAP_Interest_PDU_1_0 {
    private String sourcePeer;
    private int eraFrom;
    private int eraTo;

    public InterestPDU_Impl(int flagsInt, InputStream is) throws IOException, ASAPException {
        super(ASAP_1_0.INTEREST_CMD);

        evaluateFlags(flagsInt);

        if(this.peerSet()) { this.readPeer(is); }
        if(this.sourcePeerSet()) { this.readSourcePeer(is); }
        this.readFormat(is);
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraFromSet()) { this.readFromEra(is); }
        if(this.eraToSet()) { this.readToEra(is); }
    }

    private void readToEra(InputStream is) throws IOException, ASAPException {
        this.eraTo = this.readIntegerParameter(is);
    }

    private void readFromEra(InputStream is) throws IOException, ASAPException {
        this.eraFrom = this.readIntegerParameter(is);
    }

    private void readSourcePeer(InputStream is) throws IOException, ASAPException {
        this.sourcePeer = this.readCharSequenceParameter(is);
    }

    static void sendPDU(CharSequence peer, CharSequence sourcePeer, CharSequence format,
                        CharSequence channel, int eraFrom, int eraTo, OutputStream os,
                        boolean sign, boolean encrypted, boolean mustBeEncrypted,
                        ASAPSignAndEncryptionKeyStorage keyStorage)

            throws IOException, ASAPException {

        if(format == null || format.length() < 1) format = ASAP_1_0.ANY_FORMAT;

        // first: check protocol errors
        PDU_Impl.checkValidEra(eraFrom);
        PDU_Impl.checkValidEra(eraTo);
        PDU_Impl.checkValidFormat(format);
        PDU_Impl.checkValidSign(peer, sign);
        PDU_Impl.checkValidStream(os);

        // Basis test
        PDU_Impl.checkBasicSecurityRequirements(sign, encrypted, keyStorage);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(peer, flags, PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(sourcePeer, flags, SOURCE_PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(eraFrom, flags, ERA_FROM_BIT_POSITION);
        flags = PDU_Impl.setFlag(eraTo, flags, ERA_TO_BIT_POSITION);

        OutputStream usedOS = os; // assume we d not sign.
        ByteArrayOutputStream bufferOS = null;

        if(sign) {
            // we have to collect all bytes which are to be signed
            bufferOS = new ByteArrayOutputStream();
            usedOS = bufferOS;
        }

        PDU_Impl.sendHeader(ASAP_1_0.INTEREST_CMD, flags, os);

        PDU_Impl.sendCharSequenceParameter(peer, os); // opt
        PDU_Impl.sendCharSequenceParameter(sourcePeer, os); // opt
        PDU_Impl.sendCharSequenceParameter(format, os); // mand
        PDU_Impl.sendCharSequenceParameter(channel, os); // opt
        PDU_Impl.sendNonNegativeIntegerParameter(eraFrom, os); // opt
        PDU_Impl.sendNonNegativeIntegerParameter(eraTo, os); // opt

        if(sign) {
            // anything was written into a bytearray

            // produce signature
            Signature signature = null;
            try {
                signature = Signature.getInstance("TODO_signing_algorithm");
                signature.initSign(keyStorage.getPrivateKey()); // desperate try
                byte[] bytes2Sign = bufferOS.toByteArray();
                signature.update(bytes2Sign);
                byte[] signatureBytes = signature.sign();

                // send out anything, including signature

                // TODO need number of bytes payload to find signature later.
                os.write(bytes2Sign);
                os.write(signatureBytes);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                throw new ASAPSecurityException(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public String getSourcePeer() { return this.sourcePeer;}

    @Override
    public int getEraFrom() { return this.eraFrom; }

    @Override
    public int getEraTo() { return this.eraTo; }
}
