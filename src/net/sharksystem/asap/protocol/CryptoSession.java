package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

class CryptoSession {
    private byte cmd;
    private final OutputStream os;

    CryptoSession(byte cmd, OutputStream os, boolean sign, boolean encrypted,
                  CharSequence recipient,
                  ASAPSignAndEncryptionKeyStorage keyStorage)
            throws ASAPSecurityException {

        this.cmd = cmd;
        this.os = os;

        if(encrypted) {
            // add to command
            this.cmd += ASAP_1_0.ENCRYPTED_CMD;

            // there must be a keyStorage
            if(keyStorage == null) {
                throw new ASAPSecurityException("asap message is to be encrypted if possible " +
                        "but there is not key store at all - fatal, give up");
            }

            PublicKey publicKey = keyStorage.getPublicKey(recipient);
            // there should be an exception - but better safe than sorry
            if(publicKey == null) {
                throw new ASAPSecurityException(
                        "message must be encrypted but recipients' public key cannot be found");
            }

            // we have at least the chance
            // encryption?
            try {
                Cipher cipher = Cipher.getInstance("TODO_Cipher_Algorithm");
                cipher.init(Cipher.ENCRYPT_MODE, keyStorage.getPublicKey(recipient));

                byte[] message = new byte[0];
                cipher.doFinal(message);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            }
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

    public void sendHeader() throws IOException {
        PDU_Impl.sendCmd(this.cmd, this.os);
    }

    byte getCMD() {
        return this.cmd;
    }

    OutputStream getOutputStream() {
        return this.os;
    }

    public void finish() {

    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }
}
