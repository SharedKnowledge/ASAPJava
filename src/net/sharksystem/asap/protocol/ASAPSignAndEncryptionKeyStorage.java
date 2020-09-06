package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPSecurityException;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface ASAPSignAndEncryptionKeyStorage {
    /**
     *
     * @return private key of local device - for signing
     * @throws ASAPSecurityException
     */
    PrivateKey getPrivateKey() throws ASAPSecurityException;

    /**
     *
     * @param subjectID
     * @return public key of recipient - to encrypt
     */
    PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException;
}
