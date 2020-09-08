package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.ASAPBasicKeyStorage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

class CryptoMessage {
    private Signature signature;
    private CharSequence recipient;
    private ASAPBasicKeyStorage keyStorage;
    private Cipher cipher = null;
    private PublicKey publicKey;
    private byte cmd;

    private OutputStream effectivOS;
    private OutputStream realOS;
    private ByteArrayOutputStream asapMessageOS;
    private InputStreamCopy inputStreamCopy;
    private byte[] encryptedSymmetricKey;
    private byte[] encryptedContent;

    CryptoMessage(ASAPBasicKeyStorage keyStorage) {
        this.keyStorage = keyStorage;
    }

    CryptoMessage(byte cmd, OutputStream os, boolean sign, boolean encrypted,
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

            if(this.recipient == null) {
                throw new ASAPSecurityException("cannot encrypt message with no specified receiver - fatal, give up");
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

        // must be after signing
        if(this.cipher != null) {
            try {
                // send receiver - unencrypted - need this for ad-hoc routing
                PDU_Impl.sendCharSequenceParameter(this.recipient, this.realOS);

                // get symmetric key
                SecretKey encryptionKey = this.keyStorage.generateSymmetricKey();
                byte[] encodedSymmetricKey = encryptionKey.getEncoded();

                // encrypt key
                byte[] encryptedSymmetricKeyBytes = this.cipher.doFinal(encodedSymmetricKey);

                // send encrypted key
                this.writeByteArray(encryptedSymmetricKeyBytes, this.realOS);

                // get maybe signed asap message
                byte[] asapMessageAsBytes = this.asapMessageOS.toByteArray();

                // encrypt message with symmetric key
                try {
                    Cipher symmetricCipher = Cipher.getInstance(keyStorage.getSymmetricEncryptionAlgorithm());
                    symmetricCipher.init(Cipher.ENCRYPT_MODE, encryptionKey);

                    /*
                    // block by block
                    int i = 0;
                    while(i +  MAX_ENCRYPTION_BLOCK_SIZE < asapMessageAsBytes.length) {
                        symmetricCipher.update(asapMessageAsBytes, i, MAX_ENCRYPTION_BLOCK_SIZE);
                        i += MAX_ENCRYPTION_BLOCK_SIZE;
                    }

                    int lastStepLen = asapMessageAsBytes.length - i;
                    symmetricCipher.update(asapMessageAsBytes, i, lastStepLen);
                    // did not work - ignored previous updates. anyway, there is a solution, see below
                    byte[] encryptedContent = symmetricCipher.doFinal();
                     */

                    byte[] encryptedContent = symmetricCipher.doFinal(asapMessageAsBytes);
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

    public InputStream setupInputStreamCopier(int priorInt, InputStream is)
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
        PublicKey publicKey = this.keyStorage.getPublicKey(sender);
        if(publicKey == null) return false;

        try {
            this.signature = Signature.getInstance(this.keyStorage.getRSASigningAlgorithm());
            this.signature.initVerify(publicKey);
            // get data which are to be verified
            byte[] signedData = this.inputStreamCopy.getCopy();
            this.signature.update(signedData);
            byte[] signatureBytes = this.readByteArray(is);
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
        InputStream copyStream = this.setupInputStreamCopier(cmd, is);

        // read recipient
        this.recipient = PDU_Impl.readCharSequenceParameter(copyStream);

        // read encrypted symmetric key
        this.encryptedSymmetricKey = this.readByteArray(copyStream);

        // read content
        this.encryptedContent = this.readByteArray(copyStream);

        // read anything - are we recipient?
        if(this.keyStorage.isOwner(this.recipient)) {
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
        return this.doDecryption(is, this.keyStorage.getPrivateKey());
    }

    // parameter private key is usually not an option. Good entry for testing / debugging, though
    public InputStream doDecryption(InputStream is, PrivateKey privateKey) throws ASAPSecurityException {
        try {
            // decrypt encoded symmetric key
            this.cipher = Cipher.getInstance(keyStorage.getRSAEncryptionAlgorithm());
            this.cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // read encryptedKey in initDecryption
            byte[] encodedSymmetricKey = this.cipher.doFinal(this.encryptedSymmetricKey);

            // create symmetric key object
            SecretKey symmetricKey =
                    new SecretKeySpec(encodedSymmetricKey, this.keyStorage.getSymmetricKeyType());

            // decrypt content
            Cipher symmetricCipher = Cipher.getInstance(keyStorage.getSymmetricEncryptionAlgorithm());
            symmetricCipher.init(Cipher.DECRYPT_MODE, symmetricKey);

            // read encryptedContent in initDecryption
            byte[] decryptedBytes = symmetricCipher.doFinal(this.encryptedContent);
            return new ByteArrayInputStream(decryptedBytes);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException e) {
            throw new ASAPSecurityException(this.getLogStart(), e);
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
