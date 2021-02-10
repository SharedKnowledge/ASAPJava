package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.utils.Log;

import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

public class InMemoASAPKeyStore implements ASAPKeyStore {
    private KeyPair keyPair;
    private final CharSequence ownerID;
    private long keyPairCreationTime = 0;

    // for debugging only - we don't have private key in real apps
    private HashMap<String, KeyPair> peerKeyPairs = new HashMap<>();

    private HashMap<CharSequence, PublicKey> peerPublicKeys = new HashMap<CharSequence, PublicKey>();

    /**
     * Setup key store, A key pair will be generated
     * @param ownerID
     * @throws ASAPSecurityException
     */
    public InMemoASAPKeyStore(CharSequence ownerID) throws ASAPSecurityException {
        // generate owners key pair;
        this.ownerID = ownerID;
        //this.generateKeyPair();
    }

    /**
     * Setup key store with a key pair
     * @param ownerID
     * @param ownerKeyPair
     */
    public InMemoASAPKeyStore(CharSequence ownerID, KeyPair ownerKeyPair, long keyPairCreationTime) {
        this.ownerID = ownerID;
        this.keyPair = ownerKeyPair;
        this.keyPairCreationTime = keyPairCreationTime;
    }

    protected void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public KeyPair getKeyPair() {
        if(this.keyPair == null) {
            Log.writeLog(this, "key pair not yet generated - do it now");
            try {
                this.generateKeyPair();
            } catch (ASAPSecurityException e) {
                Log.writeLogErr(this, "cannot generate key pair - fatal");
                e.printStackTrace();
                return null;
            }
        }
        return this.keyPair;
    }

    public SecretKey generateSymmetricKey() throws ASAPSecurityException {
        return ASAPCryptoAlgorithms.generateSymmetricKey(
                DEFAULT_SYMMETRIC_KEY_TYPE, DEFAULT_SYMMETRIC_KEY_SIZE);
    }

    public long getKeysCreationTime() throws ASAPSecurityException {
        return this.keyPairCreationTime;
    }

    public int getSymmetricKeyLen() {
        return ASAPKeyStore.DEFAULT_SYMMETRIC_KEY_SIZE;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    public void generateKeyPair() throws ASAPSecurityException {
        this.setKeyPair(this.generateNewKeyPair());
        this.keyPairCreationTime = System.currentTimeMillis();
    }

    private KeyPair generateNewKeyPair() throws ASAPSecurityException {
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
        KeyPair keyPair = this.generateNewKeyPair();
        this.peerKeyPairs.put(id, keyPair);
        return keyPair;
    }

    @Override
    public PrivateKey getPrivateKey() throws ASAPSecurityException {
        if(this.keyPair == null) throw new ASAPSecurityException("private key does not exist");
        return this.keyPair.getPrivate();
    }

    @Override
    public PublicKey getPublicKey() throws ASAPSecurityException {
        if(this.keyPair == null) throw new ASAPSecurityException("private key does not exist");
        return this.keyPair.getPublic();
    }

    @Override
    public PublicKey getPublicKey(CharSequence subjectID) throws ASAPSecurityException {
        KeyPair keyPair = this.peerKeyPairs.get(subjectID);
        if(keyPair == null) {
            // try there - which is more likely
            PublicKey publicKey = this.peerPublicKeys.get(subjectID);
            if(publicKey != null) {
                // got it
                return publicKey;
            }
            // else
            throw new ASAPSecurityException("no key pair for " + subjectID);
        }

        return keyPair.getPublic();
    }

    void putPublicKey(CharSequence peer, PublicKey key) {
        this.peerPublicKeys.put(peer, key);
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
    public String getAsymmetricEncryptionAlgorithm() {
        return DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM;
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
    public boolean isOwner(CharSequence peerID) {
        return ASAPCryptoAlgorithms.sameID(this.getOwner(), peerID);
    }

    @Override
    public CharSequence getOwner() {
        return this.ownerID;
    }

    @Override
    public String getAsymmetricSigningAlgorithm() {
        return DEFAULT_ASYMMETRIC_SIGNATURE_ALGORITHM;
    }

    public void addKeyPair(String peerID, KeyPair keyPair) {

        this.peerKeyPairs.put(peerID, keyPair);
    }

    /**
     * Write storage owners' public key to stream
     * @param os
     */
    public void writePublicKey(OutputStream os) throws ASAPSecurityException, IOException {
        PublicKey publicKey = this.getPublicKey();

        String format = publicKey.getFormat();
        String algorithm = publicKey.getAlgorithm();
        byte[] byteEncodedPublicKey = publicKey.getEncoded();
        int length = byteEncodedPublicKey.length;

        // makes it much easier to serialize simple data
        DataOutputStream dos = new DataOutputStream(os);

        // write to stream
        dos.writeUTF(format);
        dos.writeUTF(algorithm);
        dos.writeInt(byteEncodedPublicKey.length);

        // write encoded key directly on stream
        os.write(byteEncodedPublicKey);
    }

    /**
     * Read public key from another peer from an extern source (input stream)
     * @param peer
     * @param is
     */
    public void readPublicKey(CharSequence peer, InputStream is) throws ASAPSecurityException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        DataInputStream dis = new DataInputStream(is);

        // read in same order as written
        String format = dis.readUTF();
        String algorithm = dis.readUTF();
        int len = dis.readInt();

        // allocate memory
        byte[] byteEncodedPublicKey = new byte[len];

        // read encoded key directly from stream
        is.read(byteEncodedPublicKey);

        // create key object

        // should be revised - why?
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(byteEncodedPublicKey);

        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);

        // store it
        this.putPublicKey(peer, publicKey);
    }
}
