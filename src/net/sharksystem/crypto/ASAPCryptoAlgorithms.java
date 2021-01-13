package net.sharksystem.crypto;

import net.sharksystem.asap.internals.ASAPException;
import net.sharksystem.asap.internals.ASAPSecurityException;
import net.sharksystem.utils.ASAPSerialization;

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
     * @param basicKeyStore
     * @return
     * @throws ASAPSecurityException
     */
    public static void writeEncryptedMessagePackage(byte[] unencryptedBytes, CharSequence recipient,
                                    BasicKeyStore basicKeyStore, OutputStream os) throws ASAPSecurityException {

        PublicKey publicKey = basicKeyStore.getPublicKey(recipient);
        // there should be an exception - but better safe than sorry
        if(publicKey == null) {
            throw new ASAPSecurityException("recipients' public key cannot be found");
        }

        try {
            // send receiver - unencrypted - need this for ad-hoc routing
            ASAPSerialization.writeCharSequenceParameter(recipient, os);

            // get symmetric key
            SecretKey encryptionKey = basicKeyStore.generateSymmetricKey();
            byte[] encodedSymmetricKey = encryptionKey.getEncoded();

            // encrypt symmetric key
            Cipher cipher = Cipher.getInstance(basicKeyStore.getAsymmetricEncryptionAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedSymmetricKeyBytes = cipher.doFinal(encodedSymmetricKey);

            // send encrypted key
            ASAPSerialization.writeByteArray(encryptedSymmetricKeyBytes, os);

            byte[] encryptedText = encryptSymmetric(unencryptedBytes, encryptionKey, basicKeyStore);

            // send encrypted bytes
            ASAPSerialization.writeByteArray(encryptedText, os);

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new ASAPSecurityException("problems when encrypting", e);
        }
    }

    public static byte[] produceEncryptedMessagePackage(byte[] unencryptedBytes, CharSequence recipient,
                    BasicKeyStore basicKeyStore) throws ASAPSecurityException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeEncryptedMessagePackage(unencryptedBytes, recipient, basicKeyStore, baos);
        // return whole block of bytes
        return baos.toByteArray();
    }

    public static SecretKey createSymmetricKey(byte[] encodedSymmetricKey,
                        BasicKeyStore basicKeyStore) {
        return new SecretKeySpec(encodedSymmetricKey, basicKeyStore.getSymmetricKeyType());
    }

    public static byte[] decryptPackage(EncryptedMessagePackage encryptedMessagePackage,
                            BasicKeyStore basicKeyStore) throws ASAPSecurityException {

        byte[] encodedSymmetricKey = decryptAsymmetric(
                encryptedMessagePackage.getEncryptedSymmetricKey(),
                basicKeyStore);

        // create symmetric key object
        SecretKey symmetricKey = createSymmetricKey(encodedSymmetricKey, basicKeyStore);


        // decrypt content
        return decryptSymmetric(encryptedMessagePackage.getEncryptedContent(), symmetricKey, basicKeyStore);
    }

    public static SecretKey generateSymmetricKey(String keyType, int size) throws ASAPSecurityException {
        try {
            KeyGenerator gen = KeyGenerator.getInstance(keyType);
            gen.init(size);
            SecretKey secretKey = gen.generateKey();
            return secretKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ASAPSecurityException(ASAPCryptoAlgorithms.class.getSimpleName(), e);
        }
    }

    public static boolean sameID(CharSequence peerA, CharSequence peerB) {
        String peerAString = peerA.toString();
        String peerBString = peerB.toString();

        return peerAString.equalsIgnoreCase(peerBString);
    }

    public interface EncryptedMessagePackage {
        CharSequence getRecipient();
        byte[] getEncryptedSymmetricKey();
        byte[] getEncryptedContent();
    }

    public static EncryptedMessagePackage parseEncryptedMessagePackage(InputStream is) throws IOException, ASAPException {
        String recipient = ASAPSerialization.readCharSequenceParameter(is);
        byte[] encryptedSymmetricKey = ASAPSerialization.readByteArray(is);
        byte[] encryptedContent = ASAPSerialization.readByteArray(is);

        return new EncryptedMessagePackage() {
            @Override
            public CharSequence getRecipient() { return recipient; }
            @Override
            public byte[] getEncryptedSymmetricKey() {   return encryptedSymmetricKey; }
            @Override
            public byte[] getEncryptedContent() { return encryptedContent; }
        };
    }

    public static void writeEncryptedMessagePackage(EncryptedMessagePackage encryptedPackage, OutputStream os)
            throws IOException, ASAPException {
        ASAPSerialization.writeCharSequenceParameter(encryptedPackage.getRecipient(), os);
        ASAPSerialization.writeByteArray(encryptedPackage.getEncryptedSymmetricKey(), os);
        ASAPSerialization.writeByteArray(encryptedPackage.getEncryptedContent(), os);
    }

    public static byte[] getEncryptedMessagePackageAsBytes(EncryptedMessagePackage encryptedPackage)
            throws IOException, ASAPException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeEncryptedMessagePackage(encryptedPackage, baos);
        return baos.toByteArray();
    }

    /**
     *
     * @param unencryptedBytes
     * @param encryptionKey
     * @return
     */
    public static byte[] encryptSymmetric(byte[] unencryptedBytes, SecretKey encryptionKey,
                          BasicKeyStore basicKeyStore) throws ASAPSecurityException {

        try {
            // encrypt message with symmetric key
            Cipher symmetricCipher = Cipher.getInstance(basicKeyStore.getSymmetricEncryptionAlgorithm());
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
            e.printStackTrace();
            throw new ASAPSecurityException("symmetric encryption failed: "
                    + basicKeyStore.getSymmetricEncryptionAlgorithm(), e);
        }
    }

    public static byte[] decryptSymmetric(byte[] encryptedContent, SecretKey symmetricKey,
                          BasicKeyStore basicKeyStore) throws ASAPSecurityException {

        try {
            Cipher symmetricCipher = Cipher.getInstance(basicKeyStore.getSymmetricEncryptionAlgorithm());
            symmetricCipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            return symmetricCipher.doFinal(encryptedContent);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new ASAPSecurityException("symmetric decryption failed: "
                    + basicKeyStore.getSymmetricEncryptionAlgorithm(), e);
        }
    }

    public static byte[] decryptAsymmetric(byte[] encryptedBytes, BasicKeyStore basicKeyStore)
            throws ASAPSecurityException {
        try {
            Cipher cipher = Cipher.getInstance(basicKeyStore.getAsymmetricEncryptionAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, basicKeyStore.getPrivateKey());
            return cipher.doFinal(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new ASAPSecurityException("asymmetric decryption failed: "
                    + basicKeyStore.getAsymmetricEncryptionAlgorithm(), e);
        }
    }

    public static byte[] sign(byte[] bytes2Sign, BasicKeyStore basicKeyStore)
            throws ASAPSecurityException {

        try {
            Signature signature = Signature.getInstance(basicKeyStore.getAsymmetricSigningAlgorithm());
            signature.initSign(basicKeyStore.getPrivateKey());
            signature.update(bytes2Sign);
            return signature.sign();
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ASAPSecurityException("signing failed: " + basicKeyStore.getAsymmetricSigningAlgorithm(), e);
        }
    }

    public static boolean verify(byte[] signedData, byte[] signatureBytes, String sender,
                        BasicKeyStore basicKeyStore) throws ASAPSecurityException {

        PublicKey publicKey = basicKeyStore.getPublicKey(sender);
        if(publicKey == null) return false;

        try {
            Signature signature = Signature.getInstance(basicKeyStore.getAsymmetricSigningAlgorithm());
            signature.initVerify(publicKey); // init with private key
            signature.update(signedData); // feed with signed data
            return signature.verify(signatureBytes); // check against signature
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            throw new ASAPSecurityException("verifying failed: " + basicKeyStore.getAsymmetricSigningAlgorithm(), e);
        }
    }
}
