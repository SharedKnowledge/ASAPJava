package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.util.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.HashMap;

public class TestASAPKeyStorage implements ASAPBasicKeyStorage {
    private final KeyPair keyPair;
    private final String name;
    private long timeInMillis = 0;

    public static final String DEFAULT_RSA_ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public static final String DEFAULT_SYMMETRIC_KEY_TYPE = "AES";
    public static final String DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";
//    public static int DEFAULT_AES_KEY_SIZE = 256; TODO we need a better one
    public static int DEFAULT_AES_KEY_SIZE = 128;
    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";

    private HashMap<String, KeyPair> peerKeyPairs = new HashMap<>();

    public TestASAPKeyStorage(String name) throws ASAPSecurityException {
        // generate owners key pair;
        this.name = name;
        this.keyPair = this.generateKeyPair();
        this.timeInMillis = System.currentTimeMillis();
    }

    public TestASAPKeyStorage(String name, KeyPair ownerKeyPair) {
        this.name = name;
        this.keyPair = ownerKeyPair;
    }

    public SecretKey generateSymmetricKey() throws ASAPSecurityException {
        try {
            KeyGenerator gen = KeyGenerator.getInstance(DEFAULT_SYMMETRIC_KEY_TYPE);
            gen.init(DEFAULT_AES_KEY_SIZE);
            SecretKey secretKey = gen.generateKey();
            return secretKey;
        } catch (NoSuchAlgorithmException e) {
            throw new ASAPSecurityException(this.getLogStart(), e);
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
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

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    @Override
    public PrivateKey getPrivateKey() throws ASAPSecurityException {
        PrivateKey aPrivate = this.keyPair.getPrivate();
        if(aPrivate == null) throw new ASAPSecurityException("private key does not exist");
        return aPrivate;
    }

    @Override
    public PublicKey getPublicKey() throws ASAPSecurityException {
        PublicKey aPublic = this.keyPair.getPublic();
        if(aPublic == null) throw new ASAPSecurityException("public key does not exist");
        return aPublic;
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
    public String getRSAEncryptionAlgorithm() {
        return DEFAULT_RSA_ENCRYPTION_ALGORITHM;
    }

    @Override
    public String getSymmetricEncryptionAlgorithm() {
        return DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM;
    }

    @Override
    public String getSymmetricKeyType() {
        return DEFAULT_SYMMETRIC_KEY_TYPE;
    }

    @Override
    public String getRSASigningAlgorithm() {
        return DEFAULT_SIGNATURE_ALGORITHM;
    }

    public void addKeyPair(String peerID, KeyPair keyPair) {
        this.peerKeyPairs.put(peerID, keyPair);
    }
}
