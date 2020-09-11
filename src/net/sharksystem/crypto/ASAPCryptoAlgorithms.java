package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
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
     * @param basicCryptoParameters
     * @return
     * @throws ASAPSecurityException
     */
    public static void writeEncryptedMessagePackage(byte[] unencryptedBytes, CharSequence recipient,
                                                    BasicCryptoParameters basicCryptoParameters, OutputStream os) throws ASAPSecurityException {

        PublicKey publicKey = basicCryptoParameters.getPublicKey(recipient);
        // there should be an exception - but better safe than sorry
        if(publicKey == null) {
            throw new ASAPSecurityException("recipients' public key cannot be found");
        }

        try {
            // send receiver - unencrypted - need this for ad-hoc routing
            ASAPSerialization.writeCharSequenceParameter(recipient, os);

            // get symmetric key
            SecretKey encryptionKey = basicCryptoParameters.generateSymmetricKey();
            byte[] encodedSymmetricKey = encryptionKey.getEncoded();

            // encrypt symmetric key
            Cipher cipher = Cipher.getInstance(basicCryptoParameters.getRSAEncryptionAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedSymmetricKeyBytes = cipher.doFinal(encodedSymmetricKey);

            // send encrypted key
            ASAPSerialization.writeByteArray(encryptedSymmetricKeyBytes, os);

            byte[] encryptedText = encryptSymmetric(unencryptedBytes, encryptionKey, basicCryptoParameters);

            // send encrypted bytes
            ASAPSerialization.writeByteArray(encryptedText, os);

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new ASAPSecurityException("problems when encrypting", e);
        }
    }

    public static byte[] produceEncryptedMessagePackage(byte[] unencryptedBytes, CharSequence recipient,
                    BasicCryptoParameters basicCryptoParameters) throws ASAPSecurityException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeEncryptedMessagePackage(unencryptedBytes, recipient, basicCryptoParameters, baos);
        // return whole block of bytes
        return baos.toByteArray();
    }

    public static SecretKey createSymmetricKey(byte[] encodedSymmetricKey,
                        BasicCryptoParameters basicCryptoParameters) {
        return new SecretKeySpec(encodedSymmetricKey, basicCryptoParameters.getSymmetricKeyType());
    }

    public static byte[] decryptPackage(EncryptedMessagePackage encryptedMessagePackage,
                            BasicCryptoParameters basicCryptoParameters) throws ASAPSecurityException {

        byte[] encodedSymmetricKey = decryptAsymmetric(
                encryptedMessagePackage.getEncryptedSymmetricKey(),
                basicCryptoParameters);

        // create symmetric key object
        SecretKey symmetricKey = createSymmetricKey(encodedSymmetricKey, basicCryptoParameters);


        // decrypt content
        return decryptSymmetric(encryptedMessagePackage.getEncryptedContent(), symmetricKey, basicCryptoParameters);
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
                          BasicCryptoParameters basicCryptoParameters) throws ASAPSecurityException {

        try {
            // encrypt message with symmetric key
            Cipher symmetricCipher = Cipher.getInstance(basicCryptoParameters.getSymmetricEncryptionAlgorithm());
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
                          BasicCryptoParameters basicCryptoParameters) throws ASAPSecurityException {

        try {
            Cipher symmetricCipher = Cipher.getInstance(basicCryptoParameters.getSymmetricEncryptionAlgorithm());
            symmetricCipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            return symmetricCipher.doFinal(encryptedContent);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new ASAPSecurityException("problems when decrypting with symmetric key", e);
        }
    }

    public static byte[] decryptAsymmetric(byte[] encryptedBytes, BasicCryptoParameters basicCryptoParameters)
            throws ASAPSecurityException {
        try {
            Cipher cipher = Cipher.getInstance(basicCryptoParameters.getRSAEncryptionAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, basicCryptoParameters.getPrivateKey());
            return cipher.doFinal(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            throw new ASAPSecurityException("problems when encrypting with symmetric key", e);
        }
    }

    public static byte[] sign(byte[] bytes2Sign, BasicCryptoParameters basicCryptoParameters)
            throws ASAPSecurityException {

        try {
            Signature signature = Signature.getInstance(basicCryptoParameters.getRSASigningAlgorithm());
            signature.initSign(basicCryptoParameters.getPrivateKey());
            signature.update(bytes2Sign);
            return signature.sign();
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            throw new ASAPSecurityException("problem when signing", e);
        }
    }

    public static boolean verify(byte[] signedData, byte[] signatureBytes, String sender,
                        BasicCryptoParameters basicCryptoParameters) throws ASAPSecurityException {

        PublicKey publicKey = basicCryptoParameters.getPublicKey(sender);
        if(publicKey == null) return false;

        try {
            Signature signature = Signature.getInstance(basicCryptoParameters.getRSASigningAlgorithm());
            signature.initVerify(publicKey); // init with private key
            signature.update(signedData); // feed with signed data
            return signature.verify(signatureBytes); // check against signature
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new ASAPSecurityException("problems when verifying", e);
        }
    }
}
