package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Defines basic methods we need to implement signing and verification and to get information how to encrypt
 */
public interface ASAPCryptoParameterStorage {
    String DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";
    String DEFAULT_SYMMETRIC_KEY_TYPE = "AES";
    String DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";
    //    public static int DEFAULT_AES_KEY_SIZE = 256;
    int DEFAULT_SYMMETRIC_KEY_SIZE = 128; // TODO we can do better
    String DEFAULT_ASYMMETRIC_SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     *
     * @return private key of local device - for signing
     * @throws ASAPSecurityException
     */
    PrivateKey getPrivateKey() throws ASAPSecurityException;

    // debugging
//    PrivateKey getPrivateKey(CharSequence subjectID) throws ASAPSecurityException;

    /**
     * @return public key of local device - for signing
     * @throws ASAPSecurityException
     */
    PublicKey getPublicKey() throws ASAPSecurityException;

    long getKeysCreationTime() throws ASAPSecurityException;

    String getAsymmetricEncryptionAlgorithm();

    String getAsymmetricSigningAlgorithm();

    SecretKey generateSymmetricKey() throws ASAPSecurityException;

    String getSymmetricEncryptionAlgorithm();

    String getSymmetricKeyType();

    int getSymmetricKeyLen();
}
