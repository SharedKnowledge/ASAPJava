package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPSecurityException;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface BasicKeyStore extends BasicCryptoSettings {
    /**
     * @param subjectID
     * @return public key of recipient - to encrypt
     * @throws ASAPSecurityException if key cannot be found
     */
    PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException;
}
