package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.util.Log;

import java.security.*;
import java.util.HashMap;

public class TestASAPKeyStorage implements ASAPReadonlyKeyStorage {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private long timeInMillis = 0;

    public static final String DEFAULT_RSA_ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public static final String DEFAULT_SIGNATURE_METHOD = "SHA256withRSA";

    private HashMap<String, KeyPair> peerKeyPairs = new HashMap<>();

    TestASAPKeyStorage() throws ASAPSecurityException {
        // generate owners key pair;
        KeyPair keyPair = this.generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        this.timeInMillis = System.currentTimeMillis();
    }

    public TestASAPKeyStorage(KeyPair ownerKeyPair) {
        this.privateKey = ownerKeyPair.getPrivate();
        this.publicKey = ownerKeyPair.getPublic();
    }

    private KeyPair generateKeyPair() throws ASAPSecurityException {
        Log.writeLog(this, "create key pair");
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new ASAPSecurityException(e.getLocalizedMessage());
        }

        SecureRandom secRandom = new SecureRandom();
        try {
            keyGen.initialize(2048, secRandom);
            return keyGen.generateKeyPair();
        }
        catch(RuntimeException re) {
            throw new ASAPSecurityException(re.getLocalizedMessage());
        }
    }

    public KeyPair createTestPeer(String id) throws ASAPSecurityException {
        KeyPair keyPair = this.generateKeyPair();
        this.peerKeyPairs.put(id, keyPair);
        return keyPair;
    }

    @Override
    public PrivateKey getPrivateKey() throws ASAPSecurityException {
        if(this.privateKey == null) throw new ASAPSecurityException("private key does not exist");
        return this.privateKey;
    }

    @Override
    public PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException {
        KeyPair keyPair = this.peerKeyPairs.get(subjectID);
        if(keyPair == null) throw new ASAPSecurityException("no key pair for " + subjectID);

        return keyPair.getPublic();
    }

    /**
     * In reality there cannot be such a method - but we are in a test.
     * @param subjectID
     * @return
     * @throws ASAPSecurityException
     */
    public PrivateKey getPrivateKey(CharSequence subjectID) throws ASAPSecurityException {
        KeyPair keyPair = this.peerKeyPairs.get(subjectID);
        if(keyPair == null) throw new ASAPSecurityException("no key pair for " + subjectID);

        return keyPair.getPrivate();
    }

    @Override
    public PublicKey getPublicKey() throws ASAPSecurityException {
        if(this.publicKey == null) throw new ASAPSecurityException("public key does not exist");
        return this.publicKey;
    }

    @Override
    public String getRSAEncryptionAlgorithm() {
        return DEFAULT_RSA_ENCRYPTION_ALGORITHM;
    }

    @Override
    public String getRSASigningAlgorithm() {
        return DEFAULT_SIGNATURE_METHOD;
    }
}
