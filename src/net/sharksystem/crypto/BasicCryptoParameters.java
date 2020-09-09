package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface BasicCryptoParameters {
    /**
     *
     * @return private key of local device - for signing
     * @throws ASAPSecurityException
     */
    PrivateKey getPrivateKey() throws ASAPSecurityException;

    // debugging
    PrivateKey getPrivateKey(CharSequence subjectID) throws ASAPSecurityException;

    /**
     *
     * @param subjectID
     * @return public key of recipient - to encrypt
     * @throws ASAPSecurityException if key cannot be found
     */
    PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException;

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

    /**
     *
     * @param peerID
     * @return true if peerID is owners' id.
     */
    boolean isOwner(CharSequence peerID);
}
