package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.BasisCryptoParameters;
import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.utils.Serialization;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

class ASAPCryptoMessage {
    private boolean encrypted;
    private boolean sign;
    private Signature signature;
    private CharSequence recipient;
    private BasisCryptoParameters basisCryptoParameters;
    private byte cmd;

    private OutputStream effectivOS;
    private OutputStream realOS;
    private ByteArrayOutputStream asapMessageOS;
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
        this.effectivOS = os; // still this one
        this.basisCryptoParameters = basisCryptoParameters;
        this.recipient = recipient;
        this.encrypted = encrypted;
        this.sign = sign;

        if(encrypted || sign) {
            // we need some basic crypto parameters
            if(basisCryptoParameters == null) {
                throw new ASAPSecurityException("cannot encrypt or sign without cryptp parameters / key store");
            }
            this.setupTempMessageStorage();
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

    private void setupTempMessageStorage() {
        if(this.asapMessageOS == null) {
            this.asapMessageOS = new ByteArrayOutputStream();
            // pud will make a detour
            this.effectivOS = this.asapMessageOS;
        }
    }

    public void sendCmd() throws IOException {
        // send cmd in clear
        PDU_Impl.sendCmd(this.cmd, this.realOS);
    }

    byte getCMD() {
        return this.cmd;
    }

    public OutputStream getOutputStream() {
        return this.effectivOS;
    }

    public void finish() throws ASAPSecurityException {
        if(this.sign) {
            try {
                // get message as bytes
                byte[] asapMessageAsBytes = this.asapMessageOS.toByteArray();
                // produce signature
                byte[] signatureBytes = ASAPCryptoAlgorithms.sign(asapMessageAsBytes, this.basisCryptoParameters);

                if(this.encrypted) {
                    // have to store it - anything will be encrypted
                    Serialization.writeByteArray(signatureBytes, this.asapMessageOS);
                } else {
                    // can write anything now
                    this.realOS.write(asapMessageAsBytes);
                    Serialization.writeByteArray(signatureBytes, this.realOS);
                }
            } catch (IOException e) {
                throw new ASAPSecurityException(this.getLogStart(), e);
            }
        }

        if(this.encrypted) {
            // get maybe signed asap message
            byte[] asapMessageAsBytes = this.asapMessageOS.toByteArray();

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

    public InputStream setupCopyStream(int priorInt, InputStream is)
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
        PublicKey publicKey = this.basisCryptoParameters.getPublicKey(sender);
        if(publicKey == null) return false;

        try {
            this.signature = Signature.getInstance(this.basisCryptoParameters.getRSASigningAlgorithm());
            this.signature.initVerify(publicKey);
            // get data which are to be verified
            byte[] signedData = this.inputStreamCopy.getCopy();
            this.signature.update(signedData);
            byte[] signatureBytes = Serialization.readByteArray(is);
            boolean wasVerified = this.signature.verify(signatureBytes);
            return wasVerified;
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new ASAPSecurityException(this.getLogStart(), e);
        }
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
        // make a copy of read data
        InputStream copyStream = this.setupCopyStream(cmd, is);

        /*
        // read recipient
        this.recipient = Serialization.readCharSequenceParameter(copyStream);

        // read encrypted symmetric key
        this.encryptedSymmetricKey = Serialization.readByteArray(copyStream);

        // read content
        this.encryptedContent = Serialization.readByteArray(copyStream);
         */

        this.encryptedMessagePackage =
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(copyStream);

        if(this.basisCryptoParameters == null) {
            System.out.println(this.getLogStart() + "no keystore set: cannot handle encrypted messages");
            return false;
        }

        // read anything - are we recipient?
//        if(this.basisCryptoParameters.isOwner(this.recipient)) {
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

    public InputStream doDecryption(InputStream is) throws ASAPSecurityException {
        return this.doDecryption(is, this.basisCryptoParameters.getPrivateKey());
    }

    // parameter private key is usually not an option. Good entry for testing / debugging, though
    public InputStream doDecryption(InputStream is, PrivateKey privateKey) throws ASAPSecurityException {
        if(this.encryptedMessagePackage == null) {
            throw new ASAPSecurityException("forgot to initialize decryption? There are no data");
        }
        /*
        // decrypt encoded symmetric key
        this.cipher = Cipher.getInstance(basisCryptoParameters.getRSAEncryptionAlgorithm());
        this.cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // read encryptedKey in initDecryption
        byte[] encodedSymmetricKey = this.cipher.doFinal(this.encryptedSymmetricKey);
         */

        byte[] encodedSymmetricKey = ASAPCryptoAlgorithms.decryptAsymmetric(
                this.encryptedMessagePackage.getEncryptedSymmetricKey(),
                this.basisCryptoParameters);

        // create symmetric key object
        SecretKey symmetricKey =
                ASAPCryptoAlgorithms.createSymmetricKey(encodedSymmetricKey, this.basisCryptoParameters);


        // decrypt content
        byte[] decryptedBytes = ASAPCryptoAlgorithms.decryptSymmetric(
                this.encryptedMessagePackage.getEncryptedContent(),
                symmetricKey,
                this.basisCryptoParameters);

        return new ByteArrayInputStream(decryptedBytes);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
