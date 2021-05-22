package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.crypto.ASAPPoint2PointCryptoSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Descriptions of ASAP protocol data units and some constants
 */
public interface ASAP_1_0 {
    byte DEFAULT_INITIAL_TTL = 6; // a small world is assumed

    int ENCRYPTED_MASK = 0x1; // 0001
    byte ENCRYPTED_CMD = 1;

    int CMD_MASK = 0x6; // 0110 - first bit tells if encrypted or not
    byte INTEREST_CMD = 0;
    byte ASSIMILATE_CMD = 2;

    String ANY_FORMAT = "ASAP_ANY_FORMAT";
    String ASAP_MANAGEMENT_FORMAT = "asap/control";
    int ERA_NOT_DEFINED = -1;

    /*
    OFFER: An peer (optional) in an range of era (optional) offers data for
    an channel (optional) in a format (mandatory)
    */

    /**
     * @param peer identifies a peer - can be null
     * @param era - current era of this peer (range 0..2^8) (-1 indicates: no information about era to be transmitted)
     * @param channel describes a channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
//    void offer(CharSequence peer, CharSequence format, CharSequence channel, int era, OutputStream os, boolean signed)
//            throws IOException, ASAPException;

    /**
     * @param recipient identifies a peer - can be null
     * @param channel describes a channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
//    void offer(CharSequence recipient, CharSequence format, CharSequence channel, OutputStream os, boolean signed)
//            throws IOException, ASAPException;

    /*
    INTEREST: Sender declares an interest for data from a peer (optional) within a range of era (optional) of a
    channel (optional) in a format (mandatory)
    */

    /**
     * @param sender identifies sender - can be null
     * @param recipient can be null - no restriction - any encountered peer will get it.
     * @param eraFrom lower limit of era range (-1 means undefined)
     * @param eraTo upper limit of era range (-1 means undefined)
     * @param channel whished / required channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, boolean signed) throws IOException, ASAPException;

    void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, ASAPPoint2PointCryptoSettings cryptoSettings) throws IOException, ASAPException;

    void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, ASAPPoint2PointCryptoSettings cryptoSettings,
                  boolean asapRoutingAllowed) throws IOException, ASAPException;

    /**
     * @param sender identifies sender - can be null
     * @param recipient can be null - no restriction - any encountered peer will get it.
     * @param channel whished / required channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                  CharSequence channel, OutputStream os) throws IOException, ASAPException;

    /**
     * @param sender identifies sender - can be null
     * @param recipient can be null - no restriction - any encountered peer will get it.
     * @param eraFrom lower limit of era range (-1 means undefined)
     * @param eraTo upper limit of era range (-1 means undefined)
     * @param channel whished / required channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param sign sign message when sending
     * @param encrypted encrypt method - of possible
     * @throws IOException
     * @throws ASAPException
     */
    void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, boolean sign, boolean encrypted)
            throws IOException, ASAPException, ASAPSecurityException;

    void interest(CharSequence sender, CharSequence recipient, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, boolean sign, boolean encrypted,
                  boolean asapRoutingAllowed, Map<String, Integer> encounterMap)
            throws IOException, ASAPException, ASAPSecurityException;

    /**
     * @param sender identifies sender - can be null
     * @param channel whished / required channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void interest(CharSequence sender, CharSequence format, CharSequence sourcePeer,
                  CharSequence channel, OutputStream os, boolean signed, boolean encrypted)
            throws IOException, ASAPException;

    /*
    ASSIMILATE: Peer (optional) issues data (mandatory) to a channel (mandatory) in a format (mandatory) of a
    era (optional)
    */

    /**
     *
     * @param sender sender (optional, can be null)
     * @param recipient wished recipient (optional, can be null)
     * @param channelUri mandatory
     * @param format mandatory
     * @param offsets applications will probably store a number of messages in a data block. This (optional) list
     *                of numbers represents the offset where a new app specific message begins.
     * @param dataIS stream from which are read to be transmitted to recipient mandatory
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void assimilate(CharSequence sender, CharSequence recipient, CharSequence format, CharSequence channelUri, int era,
                    long length, List<Long> offsets, List<ASAPHop> asapHops, InputStream dataIS, OutputStream os, boolean signed)
            throws IOException, ASAPException;


    void assimilate(CharSequence sender, CharSequence recipient, CharSequence format,
                           CharSequence channel, int era, long length, List<Long> offsets, List<ASAPHop> asapHopList,
                           InputStream dataIS,
                           OutputStream os, ASAPPoint2PointCryptoSettings secureSetting) throws IOException, ASAPException;

    /**
     *
     * @param sender sender (optional, can be null)
     * @param recipient wished recipient (optional, can be null)
     * @param channelUri mandatory
     * @param format mandatory
     * @param offsets applications will probably store a number of messages in a data block. This (optional) list
     *                of numbers represents the offset where a new app specific message begins.
     * @param dataIS stream from which are read to be transmitted to recipient mandatory
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @param encrypted encrypt or not
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void assimilate(CharSequence sender, CharSequence recipient, CharSequence format, CharSequence channelUri, int era,
                    long length, List<Long> offsets, List<ASAPHop> asapHops,
                    InputStream dataIS, OutputStream os, boolean signed, boolean encrypted)
            throws IOException, ASAPException;

    /**
     *
     * @param sender sender (optional, can be null)
     * @param recipient wished recipient (optional, can be null)
     * @param channel mandatory
     * @param format mandatory
     * @param offsets applications will probably store a number of messages in a data block. This (optional) list
     *                of numbers represents the offset where a new app specific message begins.
     * @param data which are read to be transmitted to recipient mandatory
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void assimilate(CharSequence sender, CharSequence recipient, CharSequence format, CharSequence channel, int era,
                    List<Long> offsets, List<ASAPHop> asapHops, byte[] data, OutputStream os, boolean signed)
            throws IOException, ASAPException;

    /**
     *
     * @param sender sender (optional, can be null)
     * @param recipient wished recipient (optional, can be null)
     * @param channel mandatory
     * @param format mandatory
     * @param offsets applications will probably store a number of messages in a data block. This (optional) list
     *                of numbers represents the offset where a new app specific message begins.
     * @param data which are read to be transmitted to recipient mandatory
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @param encrypted encrypt or not
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void assimilate(CharSequence sender, CharSequence recipient, CharSequence format, CharSequence channel, int era,
                    List<Long> offsets, List<ASAPHop> asapHops, byte[] data, OutputStream os, boolean signed, boolean encrypted)
            throws IOException, ASAPException;


    ASAP_PDU_1_0 readPDU(InputStream is) throws IOException, ASAPException;

}
