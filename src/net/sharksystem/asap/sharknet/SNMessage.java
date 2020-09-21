package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Set;

public interface SNMessage {
    CharSequence ANY_RECIPIENT = "SN_ANY";
    CharSequence ANONYMOUS = "SN_ANON";
    int SIGNED_MASK = 0x1;
    int ENCRYPTED_MASK = 0x2;

    /**
     * Content - can be encrypted and signed
     * @return
     */
    byte[] getContent();

    /**
     * Sender - can be encrypted and signed
     * @return
     */
    CharSequence getSender();

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
     */
    boolean verified();

    /**
     * Not part of the transferred message - just a flag that indicates if this message can be encrypted.
     * This can change over time depending on status of local PKI. Any encrypted message is unencryptable
     * if the local private key gets invalid or if this message was not sent to this local peer.
     * @return
     */
    boolean encrypted();

    /**
     * Creation date is produced when an object is serialized. It becomes part of the message.
     * @return
     * @throws ASAPException
     * @throws IOException
     */
    Timestamp getCreationTime() throws ASAPException, IOException;

    /**
     * Compare two message what creation date is earlier. It depends on local clocks. It is a hint not more.
     * Could need a better solution.
     *
     * @param message
     * @return
     * @throws ASAPException
     * @throws IOException
     */
    boolean isLaterThan(SNMessage message) throws ASAPException, IOException;
}
