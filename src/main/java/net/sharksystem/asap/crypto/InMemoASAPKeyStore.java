package net.sharksystem.asap.crypto;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.fs.ExtraData;
import net.sharksystem.utils.Log;

import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * A key store that lives entirely in memory.
 * Key pair generation for the owner is performed lazily and currently limited to RSA with a key size of 2048 bits.
 */
public class InMemoASAPKeyStore implements ASAPKeyStore {
    private static final CharSequence ASAP_KEYSTORE_MEMENTO_KEY = "asapKeyStoreMemento";
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private KeyPair keyPair;
    private CharSequence ownerID;
    private long keyPairCreationTime = 0;

    // for debugging only - we don't have private key in real apps
    private HashMap<String, KeyPair> peerKeyPairs = new HashMap<>();

    private HashMap<CharSequence, PublicKey> peerPublicKeys = new HashMap<CharSequence, PublicKey>();

    /**
     * Constructs a new key store for the given owner.
     * @param ownerID ID of this key store's owner
     */
    public InMemoASAPKeyStore(CharSequence ownerID) {
        this.ownerID = ownerID;
    }

    /**
     * Constructs a new key store for the given owner with the specified key pair.
     * @param ownerID ID of this key store's owner
     * @param ownerKeyPair key pair of the owner
     * @param keyPairCreationTime a timestamp representing the date the given key pair was created on, in milliseconds
     *                            since epoch (January 1, 1970 UTC).
     * @deprecated
     */
    public InMemoASAPKeyStore(CharSequence ownerID, KeyPair ownerKeyPair, long keyPairCreationTime) {
        this.ownerID = ownerID;
        this.keyPair = ownerKeyPair;
        if(ownerKeyPair != null) {
            this.privateKey = ownerKeyPair.getPrivate();
            this.publicKey = ownerKeyPair.getPublic();
        }

        this.keyPairCreationTime = keyPairCreationTime;
    }

    protected void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    /**
     * Returns the key pair of this store's owner.
     * If this pair does not exist yet, the store attempts to generate and store one.
     * @return the key pair. null if no pair exists and generation fails.
     * @see ASAPKeyStore#generateKeyPair()
     */
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

    @Override
    public SecretKey generateSymmetricKey() throws ASAPSecurityException {
        return ASAPCryptoAlgorithms.generateSymmetricKey(
                DEFAULT_SYMMETRIC_KEY_TYPE, DEFAULT_SYMMETRIC_KEY_SIZE);
    }

    @Override
    public long getKeysCreationTime() throws ASAPSecurityException {
        return this.keyPairCreationTime;
    }

    @Override
    public int getSymmetricKeyLen() {
        return ASAPKeyStore.DEFAULT_SYMMETRIC_KEY_SIZE;
    }

    @Override
    public void generateKeyPair() throws ASAPSecurityException {
        this.setKeyPair(this.generateNewKeyPair());
        this.privateKey = this.keyPair.getPrivate();
        this.publicKey = this.keyPair.getPublic();
        this.keyPairCreationTime = System.currentTimeMillis();
        this.save();
    }

    private KeyPair generateNewKeyPair() throws ASAPSecurityException {
        Log.writeLog(this, "create key pair");
        KeyPairGenerator keyGen;
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

    private void checkKeyPairExistence() throws ASAPSecurityException {
        if(this.keyPair == null) {
            Log.writeLog(this, "create new keypair since requested but missing");
            this.generateKeyPair();
            this.privateKey = this.keyPair.getPrivate();
            this.publicKey = this.keyPair.getPublic();
        }
    }

    @Override
    public PrivateKey getPrivateKey() throws ASAPSecurityException {
        if(this.privateKey == null) {
            this.checkKeyPairExistence();
        }
        return this.privateKey;
    }

    @Override
    public PublicKey getPublicKey() throws ASAPSecurityException {
        if(this.publicKey == null) {
            this.checkKeyPairExistence();
        }
        return this.publicKey;
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
     * Writes this store's owner's public key to the given output stream.
     * If no key exists yet, the store attempts to generate it.
     * The key's metadata is written as well to enable the reader to reconstruct it.
     * @param os the stream to write to
     * @throws ASAPSecurityException if no key exists and key generation fails
     * @throws IOException if an I/O error occurs
     * @see ASAPKeyStore#generateKeyPair()
     * @see InMemoASAPKeyStore#readPublicKey(CharSequence, InputStream)
     */
    public void writePublicKey(OutputStream os) throws ASAPSecurityException, IOException {
        PublicKey publicKey = this.getPublicKey();

        String format = publicKey.getFormat();
        String algorithm = publicKey.getAlgorithm();
        byte[] byteEncodedPublicKey = publicKey.getEncoded();

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
     * Reads a public key from the given input stream and adds it to this store.
     * @param peer ID of the public key's owner
     * @param is the stream to read from
     * @throws IOException if an I/O error occurs
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public void readPublicKey(CharSequence peer, InputStream is) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        DataInputStream dis = new DataInputStream(is);

        // read in same order as written
        String format = dis.readUTF();
        String algorithm = dis.readUTF();
        int len = dis.readInt();

        // allocate memory
        byte[] byteEncodedPublicKey = new byte[len];

        // FIXME: Method currently doesn't store the received key as per the specification.
        is.read(byteEncodedPublicKey);

        // create key object

        // should be revised - why?
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(byteEncodedPublicKey);

        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);

        // store it
        this.putPublicKey(peer, publicKey);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                persist                                                   //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private ExtraData mementoExtraData;

    public void setMementoTarget(ExtraData extraData) {
        this.mementoExtraData = extraData;
        try {
            byte[] memento = this.mementoExtraData.getExtra(ASAP_KEYSTORE_MEMENTO_KEY);
            this.restoreMemento(memento);
        } catch (IOException | SharkException e) {
            // no memento - hm, maybe was set when this keystore was already running - better save
            this.save();
        }
    }

    private void save() {
        if(this.mementoExtraData != null) {
            try {
                this.mementoExtraData.putExtra(ASAP_KEYSTORE_MEMENTO_KEY, this.getMemento());
            } catch (IOException | SharkException e) {
                Log.writeLogErr(this, "cannot write memento: " + e.getLocalizedMessage());
            }
        } else {
            Log.writeLog(this, "cannot write data - no persistent storage");
        }
    }

    /**
     * Returns a byte representation of this store's owner's key pair and related metadata.
     * If no key pair exists yet, the store attempts to create one.
     * @return a byte array holding the owner's key pair
     * @throws ASAPException if no key pair exists and key generation fails
     * @throws IOException if an I/O error occurs
     * @see ASAPKeyStore#generateKeyPair()
     * @see InMemoASAPKeyStore#restoreMemento(byte[])
     */
    public byte[] getMemento() throws ASAPException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PrivateKey privateKey = this.getPrivateKey();
        // encoded private key
        ASAPSerialization.writeByteArray(privateKey.getEncoded(), baos);
        // private key algorithm
        ASAPSerialization.writeCharSequenceParameter(privateKey.getAlgorithm(), baos);
        // private key format
        ASAPSerialization.writeCharSequenceParameter(privateKey.getFormat(), baos);

        PublicKey publicKey = this.getPublicKey();
        // public key encoded
        ASAPSerialization.writeByteArray(publicKey.getEncoded(), baos);
        // public key algorithm
        ASAPSerialization.writeCharSequenceParameter(publicKey.getAlgorithm(), baos);
        // public key format
        ASAPSerialization.writeCharSequenceParameter(publicKey.getFormat(), baos);

        // creation time
        ASAPSerialization.writeLongParameter(this.keyPairCreationTime, baos);

        // ownerID
        ASAPSerialization.writeCharSequenceParameter(this.ownerID, baos);

        return baos.toByteArray();
    }

    /**
     * Constructs a key pair from the given byte array and stores it as this key store's owner's.
     * Currently, only the RSA cryptosystem is supported.
     * @param mementoData the byte representation of the key pair
     * @throws IOException if an I/O error occurs
     * @throws ASAPException if the array's end is reached or constructing the key pair fails
     * @see InMemoASAPKeyStore#getMemento()
     */
    public void restoreMemento(byte[] mementoData) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(mementoData);

        byte[] privateKeyBytes = ASAPSerialization.readByteArray(bais);
        String privateKeyAlgorithm = ASAPSerialization.readCharSequenceParameter(bais);
        String privateKeyFormat = ASAPSerialization.readCharSequenceParameter(bais);

        byte[] publicKeyBytes = ASAPSerialization.readByteArray(bais);
        String publicKeyAlgorithm = ASAPSerialization.readCharSequenceParameter(bais);
        String publicKeyFormat = ASAPSerialization.readCharSequenceParameter(bais);

        // reproduce private key
        // as to be seen, pretty limited alg/format support yet :/
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ASAPException("cannot restore keystore from memento: " + e.getLocalizedMessage());
        }

        // reset those simple data after that critical stuff.
        this.keyPairCreationTime = ASAPSerialization.readLongParameter(bais);
        this.ownerID = ASAPSerialization.readCharSequenceParameter(bais);

        Log.writeLog(this, "restored keys");
    }
}
