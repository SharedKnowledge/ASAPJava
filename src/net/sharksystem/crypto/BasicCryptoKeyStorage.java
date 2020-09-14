package net.sharksystem.crypto;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.util.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

public class BasicCryptoKeyStorage implements BasicCryptoParameters {
    private final KeyPair keyPair;
    private final CharSequence ownerID;
    private long timeInMillis = 0;

    public static final String DEFAULT_RSA_ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public static final String DEFAULT_SYMMETRIC_KEY_TYPE = "AES";
    public static final String DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";
//    public static int DEFAULT_AES_KEY_SIZE = 256;
    public static int DEFAULT_AES_KEY_SIZE = 128; // TODO we can do better
    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";

    // for debugging only - we don't have private key in real apps
    private HashMap<String, KeyPair> peerKeyPairs = new HashMap<>();

    private HashMap<CharSequence, PublicKey> peerPublicKeys = new HashMap<CharSequence, PublicKey>();

    public BasicCryptoKeyStorage(String ownerID) throws ASAPSecurityException {
        // generate owners key pair;
        this.ownerID = ownerID;
        this.keyPair = this.generateKeyPair();
        this.timeInMillis = System.currentTimeMillis();
    }

    public BasicCryptoKeyStorage(CharSequence ownerID, KeyPair ownerKeyPair) {
        this.ownerID = ownerID;
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
    public boolean isOwner(CharSequence peerID) {
        return peerID.equals(this.ownerID);
    }

    @Override
    public String getRSASigningAlgorithm() {
        return DEFAULT_SIGNATURE_ALGORITHM;
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
