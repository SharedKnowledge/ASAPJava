package net.sharksystem.asap.protocol;

import net.sharksystem.Utils;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;

class CryptoSession {
    public static final int MAX_ENCRYPTION_BLOCK_SIZE = 240;
    private Signature signature;
    private CharSequence recipient;
    private ASAPReadonlyKeyStorage keyStorage;
    private Cipher cipher = null;
    private PublicKey publicKey;
    private byte cmd;

    private OutputStream effectivOS;
    private OutputStream realOS;
    private ByteArrayOutputStream asapMessageOS;
    private InputStreamCopy verifyStream;

    CryptoSession(ASAPReadonlyKeyStorage keyStorage) {
        this.keyStorage = keyStorage;
    }

    CryptoSession(byte cmd, OutputStream os, boolean sign, boolean encrypted,
                  CharSequence recipient,
                  ASAPReadonlyKeyStorage keyStorage)
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

    OutputStream getOutputStream() {
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

                // TODO: create AES key, encrypt with RSA, send and encrypt rest with that AES key

                // that stuff will not work.
                int i = 0;
                while(i +  MAX_ENCRYPTION_BLOCK_SIZE < asapMessageAsBytes.length) {
                    this.cipher.update(asapMessageAsBytes, i, MAX_ENCRYPTION_BLOCK_SIZE);
                    i += MAX_ENCRYPTION_BLOCK_SIZE;
                }

                int lastStepLen = asapMessageAsBytes.length - i;
                byte[] encryptedBytes = this.cipher.doFinal(asapMessageAsBytes, i, lastStepLen);

                this.writeByteArray(encryptedBytes, this.realOS);
                this.realOS.write(encryptedBytes);
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

    InputStream decrypt(InputStream is) throws ASAPSecurityException {
        return this.decrypt(is, this.keyStorage.getPrivateKey());
    }

    // parameter private key is usually no option. There is just one - burt was good for debugging
    private InputStream decrypt(InputStream is, PrivateKey privateKey) throws ASAPSecurityException {
        try {
            this.cipher = Cipher.getInstance(keyStorage.getRSAEncryptionAlgorithm());
            this.cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // read encrypted message
            byte[] messageBytes = this.readByteArray(is);

            /*
            // read len
            int len = PDU_Impl.readIntegerParameter(is);
            byte[] messageBytes = new byte[len];

            // read encrypted bytes from stream
            is.read(messageBytes);
             */

            // decrypt
            byte[] decryptedBytes = this.cipher.doFinal(messageBytes);
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
