package net.sharksystem.asap.protocol;

import net.sharksystem.Utils;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

class CryptoSession {
    private CharSequence recipient;
    private ASAPReadonlyKeyStorage keyStorage;
    private Cipher cipher = null;
    private PublicKey publicKey;
    private byte cmd;

    private OutputStream effectivOS;
    private OutputStream realOS;
    private ByteArrayOutputStream asapMessageOS;
    private byte[] asapMessageAsBytes;
    private ByteArrayOutputStream senderSiteTest;
    private byte[] byte2Send;

    CryptoSession(ASAPReadonlyKeyStorage keyStorage) {
        this.keyStorage = keyStorage;
    }

    InputStream decrypt(InputStream is) throws ASAPSecurityException {
        return this.decrypt(is, this.keyStorage.getPrivateKey());
    }

    private InputStream decrypt(InputStream is, PrivateKey privateKey) throws ASAPSecurityException {
        try {
            this.cipher = Cipher.getInstance(keyStorage.getRSAEncryptionAlgorithm());
            this.cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // read len
            int len = PDU_Impl.readIntegerParameter(is);
            byte[] messageBytes = new byte[len];

            // read encrypted bytes from stream
            is.read(messageBytes);

            Utils.compareArrays(messageBytes, this.byte2Send);

            // decrypt
            byte[] decryptedBytes = this.cipher.doFinal(messageBytes);
            return new ByteArrayInputStream(decryptedBytes);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                NoSuchPaddingException | InvalidKeyException | IOException | ASAPException e) {
            throw new ASAPSecurityException(this.getLogStart(), e);
        }
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
            this.asapMessageOS = new ByteArrayOutputStream();
            // pud will make a detour
            this.effectivOS = this.asapMessageOS;
        }


        /*
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

         */


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

    public void finish() throws ASAPSecurityException {
        if(cipher != null) {
            // we are to encrypt
            this.asapMessageAsBytes = this.asapMessageOS.toByteArray();
            try {
                byte[] encryptedBytes = this.cipher.doFinal(this.asapMessageAsBytes);
                // write data len

                // write len
                PDU_Impl.sendNonNegativeIntegerParameter(encryptedBytes.length, this.realOS);
                // write data
                this.realOS.write(encryptedBytes);

                // debug - decrypt

                // make a test
                this.senderSiteTest = new ByteArrayOutputStream();
                PDU_Impl.sendNonNegativeIntegerParameter(encryptedBytes.length, senderSiteTest);
                senderSiteTest.write(encryptedBytes);
                PrivateKey privateKey = this.keyStorage.getPrivateKey(this.recipient);
                this.byte2Send = encryptedBytes;
                ByteArrayInputStream decrypt = (ByteArrayInputStream) this.decrypt(new ByteArrayInputStream(senderSiteTest.toByteArray()), privateKey);
                int i = 42;
                /*
 */
            } catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
                throw new ASAPSecurityException(this.getLogStart(), e);
            }
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
