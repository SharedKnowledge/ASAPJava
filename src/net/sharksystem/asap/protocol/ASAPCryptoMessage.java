package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.BasisCryptoParameters;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.utils.Serialization;

import java.io.*;

class ASAPCryptoMessage {
    private boolean encrypted;
    private boolean sign;
    private CharSequence recipient;
    private BasisCryptoParameters basisCryptoParameters;
    private byte cmd;

    private OutputStream effectiveOS;
    private OutputStream realOS;
    private ByteArrayOutputStream outputStreamCopy;
    private InputStreamCopy inputStreamCopy;
    private ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage;

    ASAPCryptoMessage(BasisCryptoParameters basisCryptoParameters) {
        this.basisCryptoParameters = basisCryptoParameters;
    }

    ASAPCryptoMessage(byte cmd, OutputStream os, boolean sign, boolean encrypted,
                      CharSequence recipient,
                      BasisCryptoParameters basisCryptoParameters)
            throws ASAPSecurityException {

        this.cmd = cmd;
        this.realOS = os;
        this.effectiveOS = os; // still this one
        this.basisCryptoParameters = basisCryptoParameters;
        this.recipient = recipient;
        this.encrypted = encrypted;
        this.sign = sign;

        if(encrypted || sign) {
            // we need some basic crypto parameters
            if(basisCryptoParameters == null) {
                throw new ASAPSecurityException("cannot encrypt or sign without cryptp parameters / key store");
            }
            this.setupCopyOutputStream();
        }

        if(encrypted) {
            // mark encryption in command - rest will be encrypted
            this.cmd += ASAP_1_0.ENCRYPTED_CMD;
            if(this.recipient == null) {
                throw new ASAPSecurityException("cannot encrypt message with no specified receiver - fatal, give up");
            }
        }

        if(sign) {
            // signing needs a private key - check of available
            if(basisCryptoParameters.getPrivateKey() == null) {
                throw new ASAPSecurityException("asap message is to be signed but no private key - fatal, give up");
            }
        }
    }

    private void setupCopyOutputStream() {
        if(this.outputStreamCopy == null) {
            this.outputStreamCopy = new ByteArrayOutputStream();
            // pud will make a detour
            this.effectiveOS = this.outputStreamCopy;
        }
    }

    public void sendCmd() throws IOException {
        // send cmd in clear
        PDU_Impl.sendCmd(this.cmd, this.realOS);
    }

    public OutputStream getOutputStream() {
        return this.effectiveOS;
    }

    public void finish() throws ASAPSecurityException {
        if(this.sign) {
            try {
                // get message as bytes
                byte[] asapMessageAsBytes = this.outputStreamCopy.toByteArray();
                // produce signature
                byte[] signatureBytes = ASAPCryptoAlgorithms.sign(asapMessageAsBytes, this.basisCryptoParameters);

                if(this.encrypted) {
                    // have to store it - message and signature will be encrypted
                    Serialization.writeByteArray(signatureBytes, this.outputStreamCopy);
                } else {
                    // no encryption planned - write clear to stream
                    this.realOS.write(asapMessageAsBytes);
                    Serialization.writeByteArray(signatureBytes, this.realOS);
                }
            } catch (IOException e) {
                throw new ASAPSecurityException(this.getLogStart(), e);
            }
        }

        if(this.encrypted) {
            // get maybe signed asap message
            byte[] asapMessageAsBytes = this.outputStreamCopy.toByteArray();

            ASAPCryptoAlgorithms.writeEncryptedMessagePackage(
                    asapMessageAsBytes, this.recipient, this.basisCryptoParameters, this.realOS);
        }
    }

    public CharSequence getReceiver() {
        return this.recipient;
    }

    ////////////////////////////////// verify
    private class InputStreamCopy extends InputStream {
        private final InputStream is;
        ByteArrayOutputStream copy = new ByteArrayOutputStream();

        InputStreamCopy(byte[] bytes, InputStream is) throws IOException {
            // add byte if any
            if(bytes != null && bytes.length > 0) {
                copy.write(bytes);
            }

            this.is = is;
        }

        @Override
        public int read() throws IOException {
            int read = is.read();
            copy.write(read);
            return read;
        }

        byte[] getCopy() {
            return copy.toByteArray();
        }
    }

    public InputStream setupCopyInputStream(int priorInt, InputStream is)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        if(writeInt) {
//            PDU_Impl.sendFlags(priorInt, baos);
//        }

        baos.write(priorInt);

        this.inputStreamCopy = new InputStreamCopy(baos.toByteArray(), is);
        return this.inputStreamCopy;
    }

    public boolean verify(String sender, InputStream is) throws IOException, ASAPException {
        // try to get senders' public key
        byte[] signedData = this.inputStreamCopy.getCopy();
        byte[] signatureBytes = Serialization.readByteArray(is);
        // debug break
        boolean wasVerified =
                ASAPCryptoAlgorithms.verify(signedData, signatureBytes, sender, this.basisCryptoParameters);

        return wasVerified;
    }

    ////////////////////////////////// decrypt

    /**
     * Simple idea: We read anything from stream and keep a copy. Later, we figure out
     * if we can encrypt that message or not. Either way, we can keep and redistribute a copy.
     *
     *
     * @param cmd
     * @param is
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    public boolean initDecryption(byte cmd, InputStream is) throws IOException, ASAPException {
        // make a copy of encrypted message - it is redundant. Same data in encryptedMessagePackage
        InputStream copyStream = this.setupCopyInputStream(cmd, is);

        this.encryptedMessagePackage =
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(copyStream);

        if(this.basisCryptoParameters == null) {
            System.out.println(this.getLogStart() + "no keystore set: cannot handle encrypted messages");
            return false;
        }

        if(this.basisCryptoParameters.isOwner(this.encryptedMessagePackage.getRecipient())) {
            return true;
        }

        return false;
    }

    byte[] getEncryptedMessage() throws ASAPSecurityException {
        if(this.inputStreamCopy == null) {
            throw new ASAPSecurityException(
                    this.getLogStart() + "no copy made, maybe forgot to initialize decryption?");
        }

        return this.inputStreamCopy.getCopy();
    }

    public InputStream doDecryption() throws ASAPSecurityException {
        if(this.encryptedMessagePackage == null) {
            throw new ASAPSecurityException("forgot to initialize decryption? There are no data");
        }

        byte[] decryptedBytes =
                ASAPCryptoAlgorithms.decryptPackage(this.encryptedMessagePackage, this.basisCryptoParameters);

        return new ByteArrayInputStream(decryptedBytes);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
