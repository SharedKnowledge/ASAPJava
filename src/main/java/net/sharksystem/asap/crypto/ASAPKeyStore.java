package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.fs.ExtraData;

import java.security.PublicKey;

/**
 * A key store provides access to local key pairs as well as utilities for managing
 * the key pair of the store's owner.
 * <p>
 * Key stores are required for encryption/ decryption as well as creating
 * and verifying signatures, see e.g. {@link ASAPCryptoAlgorithms}.
 */
public interface ASAPKeyStore extends ASAPCryptoParameterStorage {
    /**
     * Returns the public key of the specified peer.
     * @param subjectID ID of the peer
     * @return public key of the peer whose ID matches the given ID
     * @throws ASAPSecurityException if the requested key is not present in the key store
     */
    PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException;

    /**
     * Determines if a peer is the owner of this key store.
     * @param peerID ID of the peer to test
     * @return true if and only if the specified ID matches this key store's owner's ID
     */
    boolean isOwner(CharSequence peerID);

    CharSequence getOwner();

    /**
     * Generates a new key pair for this key store's owner and stores it.
     * @throws ASAPSecurityException if key generation fails, e.g. this key store's cryptographic algorithm
     *                                  is not available in the current environment or an invalid key size is used
     */
    void generateKeyPair() throws ASAPSecurityException;

    /**
     * Updates the persistence backend for this key store.
     * @param extraData the persistence interface to use
     */
    void setMementoTarget(ExtraData extraData);
}
