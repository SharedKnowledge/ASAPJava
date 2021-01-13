package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.internals.ASAPException;
import net.sharksystem.asap.internals.ASAPSecurityException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Set;

public interface SNMessage {
    String ANY_RECIPIENT = "SN_ANY";
    String ANONYMOUS = "SN_ANON";
    int SIGNED_MASK = 0x1;
    int ENCRYPTED_MASK = 0x2;

    /**
     * Content - can be encrypted and signed
     * @return
     * @throws ASAPSecurityException if message could not be encrypted
     */
    byte[] getContent() throws ASAPSecurityException;

    /**
     * Sender - can be encrypted and signed
     * @return
     * @throws ASAPSecurityException if message could not be encrypted
     */
    CharSequence getSender() throws ASAPSecurityException;

    /**
     * Recipients are always visible - the only recipient is in the unencrypted head if message
     * is encrypted - maybe we change this in a later version.
     * @return
     */
    Set<CharSequence> getRecipients();

    /**
     * Not part of the transferred message - just a flag that indicates if this message could now
     * be verified. This can change over time, though. A non-verifiable message can be verified if the
     * right certificate arrives. A verifiable message can become non-verifiable due to loss of certificates
     * validity. In short: Result can change state of your local PKI
     * @return
     * @throws ASAPSecurityException if message could not be encrypted
     */
    boolean verified() throws ASAPSecurityException;

    /**
     * Not part of the transferred message - just a flag that indicates if this message can be encrypted.
     * This can change over time depending on status of local PKI. Any encrypted message is unencryptable
     * if the local private key gets invalid or if this message was not sent to this local peer.
     * @return
     */
    boolean encrypted();

    /**
     *
     * @return true if this message was not encrypted in the first place or could be encrypted.
     * false. We have an encrypted message and cannot read it. We know its receiver, though.
     */
    boolean couldBeDecrypted();

    /**
     * Creation date is produced when an object is serialized. It becomes part of the message.
     * @return
     * @throws ASAPException
     * @throws IOException
     * @throws ASAPSecurityException if message could not be encrypted
     */
    Timestamp getCreationTime() throws ASAPException, ASAPSecurityException, IOException;

    /**
     * Compare two message what creation date is earlier. It depends on local clocks. It is a hint not more.
     * Could need a better solution.
     *
     * @param message
     * @return
     * @throws ASAPException
     * @throws IOException
     * @throws ASAPSecurityException if message could not be encrypted
     */
    boolean isLaterThan(SNMessage message) throws ASAPException, ASAPSecurityException, IOException;
}
