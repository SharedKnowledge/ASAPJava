package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface BasicCryptoSettings {
    String DEFAULT_RSA_ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";
    String DEFAULT_SYMMETRIC_KEY_TYPE = "AES";
    String DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";
    //    public static int DEFAULT_AES_KEY_SIZE = 256;
    int DEFAULT_AES_KEY_SIZE = 128; // TODO we can do better
    String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";

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

    String getRSAEncryptionAlgorithm();

    String getRSASigningAlgorithm();

    SecretKey generateSymmetricKey() throws ASAPSecurityException;

    String getSymmetricEncryptionAlgorithm();

    String getSymmetricKeyType();

    int getSymmetricKeyLen();
}
