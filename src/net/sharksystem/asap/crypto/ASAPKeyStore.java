package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPSecurityException;

import java.security.PublicKey;

/**
 * Basic requirement to encrypt / decrypt and sign/verify messages.
 */
public interface ASAPKeyStore extends ASAPCryptoParameterStorage {
    /**
     * @param subjectID
     * @return public key of recipient - to encrypt
     * @throws ASAPSecurityException if key cannot be found
     */
    PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException;

    /**
     *
     * @param peerID
     * @return true if peerID is owners' id.
     */
    boolean isOwner(CharSequence peerID);

    CharSequence getOwner();

    /**
     * A new key pair is created
     */
    void generateKeyPair() throws ASAPSecurityException;
}
