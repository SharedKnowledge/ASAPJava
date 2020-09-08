package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.PDU_Impl;
import net.sharksystem.crypto.ASAPBasicKeyStorage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

class CryptoSession {
    private static final int MAX_ENCRYPTION_BLOCK_SIZE = 128;
    private Signature signature;
    private CharSequence recipient;
    private ASAPBasicKeyStorage keyStorage;
    private Cipher cipher = null;
    private PublicKey publicKey;
    private byte cmd;

    private OutputStream effectivOS;
    private OutputStream realOS;
    private ByteArrayOutputStream asapMessageOS;
    private InputStreamCopy verifyStream;

    CryptoSession(ASAPBasicKeyStorage keyStorage) {
        this.keyStorage = keyStorage;
    }

    CryptoSession(byte cmd, OutputStream os, boolean sign, boolean encrypted,
                  CharSequence recipient,
                  ASAPBasicKeyStorage keyStorage)
            throws ASAPSecurityException {

        this.cmd = cmd;
        this.realOS = os;
        this.effectivOS = os; // still this one
        this.keyStorage = keyStorage;
        this.recipient = recipient;

        if(encrypted) {
            // add to command
            this.cmd += ASAP_1_0.ENCRYPTED_CMD;

            // there must be a keyStorage
            if(keyStorage == null) {
                throw new ASAPSecurityException("asap message is to be encrypted if possible " +
                        "but there is not key store at all - fatal, give up");
            }

            this.publicKey = keyStorage.getPublicKey(recipient);
            // there should be an exception - but better safe than sorry
            if(this.publicKey == null) {
                throw new ASAPSecurityException(
                        "message must be encrypted but recipients' public key cannot be found");
            }

            // let's see if we can setup cipher
            try {
                this.cipher = Cipher.getInstance(keyStorage.getRSAEncryptionAlgorithm());
                this.cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
                throw new ASAPSecurityException(this.getLogStart(), e);
            }

            // cipher is ready - we can encrypt
            this.setupTempMessageStorage();
        }

        if(sign) {
            // there must be a keyStorage
            if(keyStorage == null) {
                throw new ASAPSecurityException("asap message is to be signed but there is not key store - fatal, give up");
            }

            // signing needs a private key - check of available
            if(keyStorage.getPrivateKey() == null) {
                // assume, an exception already documented lack of a private key. if not
                throw new ASAPSecurityException("asap message is to be signed but no private key - fatal, give up");
            }

            // ok, we can sign

            // produce signature
            try {
                this.signature = Signature.getInstance(this.keyStorage.getRSASigningAlgorithm());

                // we could sign
                this.setupTempMessageStorage();

            } catch (NoSuchAlgorithmException e) {
                throw new ASAPSecurityException(e.getLocalizedMessage());
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

    private void writeByteArray(byte[] bytes2Write, OutputStream os) throws IOException {
        PDU_Impl.sendNonNegativeIntegerParameter(bytes2Write.length, os);
        os.write(bytes2Write);
    }

    private byte[] readByteArray(InputStream is) throws IOException, ASAPException {
        // read len
        int len = PDU_Impl.readIntegerParameter(is);
        byte[] messageBytes = new byte[len];

        // read encrypted bytes from stream
        is.read(messageBytes);

        return messageBytes;
    }

    public void finish() throws ASAPSecurityException {
        // signing must come first
        if(this.signature != null) {
            try {
                byte[] asapMessageAsBytes = this.asapMessageOS.toByteArray();
                this.signature.initSign(this.keyStorage.getPrivateKey());
                this.signature.update(asapMessageAsBytes);
                byte[] signatureBytes = signature.sign();

                if(this.cipher != null) {
                    // have to store it - anything will be encrypted
                    this.writeByteArray(signatureBytes, this.asapMessageOS);
                } else {
                    // can write anything now
                    this.realOS.write(asapMessageAsBytes);
                    this.writeByteArray(signatureBytes, this.realOS);
                }
            } catch (InvalidKeyException | SignatureException | IOException e) {
                throw new ASAPSecurityException(this.getLogStart(), e);
            }
        }

        if(this.cipher != null) {
            try {
                // encrypted asap message
                byte[] asapMessageAsBytes = this.asapMessageOS.toByteArray();

                // get symmetric key
                SecretKey encryptionKey = this.keyStorage.generateSymmetricKey();
                byte[] encodedSymmetricKey = encryptionKey.getEncoded();

                // encrypt key
                byte[] encryptedSymmetricKeyBytes = this.cipher.doFinal(encodedSymmetricKey);

                // send encrypted key
                this.writeByteArray(encryptedSymmetricKeyBytes, this.realOS);

                // encrypt message with symmetric key
                try {
                    this.cipher = Cipher.getInstance(keyStorage.getSymmetricEncryptionAlgorithm());
                    this.cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);

                    // block by block
                    int i = 0;
                    while(i +  MAX_ENCRYPTION_BLOCK_SIZE < asapMessageAsBytes.length) {
                        this.cipher.update(asapMessageAsBytes, i, MAX_ENCRYPTION_BLOCK_SIZE);
                        i += MAX_ENCRYPTION_BLOCK_SIZE;
                    }

                    int lastStepLen = asapMessageAsBytes.length - i;
                    byte[] encryptedContent = this.cipher.doFinal(asapMessageAsBytes, i, lastStepLen);

                    this.writeByteArray(encryptedContent, this.realOS);
                } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
                    throw new ASAPSecurityException(this.getLogStart(), e);
                }
            } catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
                throw new ASAPSecurityException(this.getLogStart(), e);
            }
        }
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

    public InputStream setupInputStreamListener(InputStream is, int flagsInt) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PDU_Impl.sendFlags(flagsInt, baos);

        this.verifyStream = new InputStreamCopy(baos.toByteArray(), is);

        return this.verifyStream;
    }

    public boolean verify(String sender, InputStream is) throws IOException, ASAPException {
        // try to get senders' public key
        PublicKey publicKey = this.keyStorage.getPublicKey(sender);
        if(publicKey == null) return false;

        try {
            this.signature = Signature.getInstance(this.keyStorage.getRSASigningAlgorithm());
            this.signature.initVerify(publicKey);
            // get data which are to be verified
            byte[] signedData = this.verifyStream.getCopy();
            this.signature.update(signedData);
            byte[] signatureBytes = this.readByteArray(is);
            boolean wasVerified = this.signature.verify(signatureBytes);
            return wasVerified;
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new ASAPSecurityException(this.getLogStart(), e);
        }
    }

    ////////////////////////////////// decrypt

    public InputStream decrypt(InputStream is) throws ASAPSecurityException {
        return this.decrypt(is, this.keyStorage.getPrivateKey());
    }

    // parameter private key is usually not an option. Good entry for testing / debugging, though
    private InputStream decrypt(InputStream is, PrivateKey privateKey) throws ASAPSecurityException {
        try {
            // read encrypted symmetric key
            byte[] encryptedSymmetricKey = this.readByteArray(is);

            // decrypt encoded symmetric key
            this.cipher = Cipher.getInstance(keyStorage.getRSAEncryptionAlgorithm());
            this.cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encodedSymmetricKey = this.cipher.doFinal(encryptedSymmetricKey);

            // create symmetric key object
            SecretKey symmetricKey =
                    new SecretKeySpec(encodedSymmetricKey, this.keyStorage.getSymmetricKeyType());

            // read content
            byte[] encryptedContent = this.readByteArray(is);

            // decrypt content
            Cipher symmetricCipher = Cipher.getInstance(keyStorage.getSymmetricEncryptionAlgorithm());
            symmetricCipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decryptedBytes = symmetricCipher.doFinal(encryptedContent);
            return new ByteArrayInputStream(decryptedBytes);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException | IOException | ASAPException e) {
            throw new ASAPSecurityException(this.getLogStart(), e);
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
