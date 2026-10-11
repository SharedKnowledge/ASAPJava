package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Basic methods and constants necessary for encryption/ decryption and signing/ verification.
 */
public interface ASAPCryptoParameterStorage {
    String DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";
    String DEFAULT_SYMMETRIC_KEY_TYPE = "AES";
    String DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";
    //    public static int DEFAULT_AES_KEY_SIZE = 256;
    int DEFAULT_SYMMETRIC_KEY_SIZE = 128; // TODO we can do better
    String DEFAULT_ASYMMETRIC_SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Returns this device's private key, generating a key pair if none is available yet.
     * @return the private key
     * @throws ASAPSecurityException if no key is available and key generation fails
     * @see ASAPKeyStore#generateKeyPair()
     */
    PrivateKey getPrivateKey() throws ASAPSecurityException;

    // debugging
//    PrivateKey getPrivateKey(CharSequence subjectID) throws ASAPSecurityException;

    /**
     * Returns this device's public key, generating a key pair if none is available yet.
     * @return the public key
     * @throws ASAPSecurityException if no key is available and key generation fails
     * @see ASAPKeyStore#generateKeyPair()
     */
    PublicKey getPublicKey() throws ASAPSecurityException;

    long getKeysCreationTime() throws ASAPSecurityException;

    String getAsymmetricEncryptionAlgorithm();

    String getAsymmetricSigningAlgorithm();

    /**
     * Generates a symmetric key using this store's default symmetric encryption algorithm
     * @return the symmetric key
     * @throws ASAPSecurityException if the default encryption algorithm is not available in the current environment
     */
    SecretKey generateSymmetricKey() throws ASAPSecurityException;

    String getSymmetricEncryptionAlgorithm();

    String getSymmetricKeyType();

    int getSymmetricKeyLen();
}
