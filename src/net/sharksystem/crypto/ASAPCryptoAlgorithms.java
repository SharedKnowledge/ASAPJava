package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.utils.Serialization;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;

public class ASAPCryptoAlgorithms {
    /**
     * Produces a byte array that consists of:
     * RecipientID | asymmetrically encrypted symmetric key | symmetrically encrypted text
     * @param unencryptedBytes text to be encrypted
     * @param recipient
     * @param basisCryptoParameters
     * @return
     * @throws ASAPSecurityException
     */
    public static void writeEncryptedMessagePackage(byte[] unencryptedBytes, CharSequence recipient,
                        BasisCryptoParameters basisCryptoParameters, OutputStream os) throws ASAPSecurityException {

        PublicKey publicKey = basisCryptoParameters.getPublicKey(recipient);
        // there should be an exception - but better safe than sorry
        if(publicKey == null) {
            throw new ASAPSecurityException("recipients' public key cannot be found");
        }

        try {
            // send receiver - unencrypted - need this for ad-hoc routing
            Serialization.writeCharSequenceParameter(recipient, os);

            // get symmetric key
            SecretKey encryptionKey = basisCryptoParameters.generateSymmetricKey();
            byte[] encodedSymmetricKey = encryptionKey.getEncoded();

            // encrypt symmetric key
            Cipher cipher = Cipher.getInstance(basisCryptoParameters.getRSAEncryptionAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedSymmetricKeyBytes = cipher.doFinal(encodedSymmetricKey);

            // send encrypted key
            Serialization.writeByteArray(encryptedSymmetricKeyBytes, os);

            byte[] encryptedText = encryptSymmetric(unencryptedBytes, encryptionKey, basisCryptoParameters);

            // send encrypted bytes
            Serialization.writeByteArray(encryptedText, os);

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new ASAPSecurityException("problems when encrypting", e);
        }
    }

    public static byte[] produceEncryptedMessagePackage(byte[] unencryptedBytes, CharSequence recipient,
                    BasisCryptoParameters basisCryptoParameters) throws ASAPSecurityException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeEncryptedMessagePackage(unencryptedBytes, recipient, basisCryptoParameters, baos);
        // return whole block of bytes
        return baos.toByteArray();
    }

    public static SecretKey createSymmetricKey(byte[] encodedSymmetricKey,
                        BasisCryptoParameters basisCryptoParameters) {
        return new SecretKeySpec(encodedSymmetricKey, basisCryptoParameters.getSymmetricKeyType());
    }

    public interface EncryptedMessagePackage {
        CharSequence getRecipient();
        byte[] getEncryptedSymmetricKey();
        byte[] getEncryptedContent();
    }

    public static EncryptedMessagePackage parseEncryptedMessagePackage(InputStream is) throws IOException, ASAPException {
        String recipient = Serialization.readCharSequenceParameter(is);
        byte[] encryptedSymmetricKey = Serialization.readByteArray(is);
        byte[] encryptedContent = Serialization.readByteArray(is);

        return new EncryptedMessagePackage() {
            @Override
            public CharSequence getRecipient() { return recipient; }
            @Override
            public byte[] getEncryptedSymmetricKey() {   return encryptedSymmetricKey; }
            @Override
            public byte[] getEncryptedContent() { return encryptedContent; }
        };
    }

    /**
     *
     * @param unencryptedBytes
     * @param encryptionKey
     * @return
     */
    public static byte[] encryptSymmetric(byte[] unencryptedBytes, SecretKey encryptionKey,
                          BasisCryptoParameters basisCryptoParameters) throws ASAPSecurityException {

        try {
            // encrypt message with symmetric key
            Cipher symmetricCipher = Cipher.getInstance(basisCryptoParameters.getSymmetricEncryptionAlgorithm());
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

            // encrypt text with symmetric key
            return symmetricCipher.doFinal(unencryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                    | IllegalBlockSizeException | BadPaddingException e) {
            throw new ASAPSecurityException("problems when encrypting with symmetric key", e);
        }
    }

    public static byte[] decryptSymmetric(byte[] encryptedContent, SecretKey symmetricKey,
                          BasisCryptoParameters basisCryptoParameters) throws ASAPSecurityException {

        try {
            Cipher symmetricCipher = Cipher.getInstance(basisCryptoParameters.getSymmetricEncryptionAlgorithm());
            symmetricCipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            return symmetricCipher.doFinal(encryptedContent);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new ASAPSecurityException("problems when encrypting with symmetric key", e);
        }
    }

    public static byte[] decryptAsymmetric(byte[] encryptedBytes, BasisCryptoParameters basisCryptoParameters) throws ASAPSecurityException {
        try {
            Cipher cipher = Cipher.getInstance(basisCryptoParameters.getRSAEncryptionAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, basisCryptoParameters.getPrivateKey());
            return cipher.doFinal(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new ASAPSecurityException("problems when encrypting with symmetric key", e);
        }
    }

    public static byte[] sign(byte[] bytes2Sign, BasisCryptoParameters basisCryptoParameters)
            throws ASAPSecurityException {

        try {
            Signature signature = Signature.getInstance(basisCryptoParameters.getRSASigningAlgorithm());
            signature.initSign(basisCryptoParameters.getPrivateKey());
            signature.update(bytes2Sign);
            return signature.sign();
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            throw new ASAPSecurityException("problem when signing", e);
        }
    }

    public static byte[] verify() {return null;}
}
