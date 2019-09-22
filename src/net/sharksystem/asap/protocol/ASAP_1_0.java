package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Descriptions of ASAP protocol data units and some constants
 */
public interface ASAP_1_0 {
    byte OFFER_CMD = 'O';
    byte INTEREST_CMD = 'I';
    byte ASSIMILATE_CMD = 'A';
    CharSequence ANY_FORMAT = "any_asap";
    CharSequence ASAP_MANAGEMENT_FORMAT = "asap/control";

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
    void offer(CharSequence peer, CharSequence format, CharSequence channel, int era, OutputStream os, boolean signed)
            throws IOException, ASAPException;

    /**
     * @param peer identifies a peer - can be null
     * @param channel describes a channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void offer(CharSequence peer, CharSequence format, CharSequence channel, OutputStream os, boolean signed)
            throws IOException, ASAPException;

    /*
    INTEREST: Sender declares an interest for data from a peer (optional) within a range of era (optional) of a
    channel (optional) in a format (mandatory)
    */

    /**
     * @param peer identifies a peer - can be null
     * @param sourcePeer wished source (authority) of information (optional, can be null)
     * @param eraFrom lower limit of era range (-1 means undefined)
     * @param eraTo upper limit of era range (-1 means undefined)
     * @param channel whished / required channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void interest(CharSequence peer, CharSequence sourcePeer, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, boolean signed) throws IOException, ASAPException;

    /**
     * @param peer wished source (authority) of information
     * @param channel whished / required channel (can be null)
     * @param format describes format - used to describe an application that can deal with transmitted data format.
     * @param os stream that PDU is to be sent
     * @param signed message is signed
     * @throws IOException exception during writing on stream
     * @throws ASAPException protocol exception: mandatory parameter missing, invalid combination of parameters, ..
     */
    void interest(CharSequence peer, CharSequence format, CharSequence sourcePeer, CharSequence channel,
                  OutputStream os, boolean signed) throws IOException, ASAPException;

    /*
    ASSIMILATE: Peer (optional) issues data (mandatory) to a channel (mandatory) in a format (mandatory) of a
    era (optional)
    */

    /**
     *
     * @param peer sender (optional, can be null)
     * @param recipientPeer wished recipient (optional, can be null)
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
    void assimilate(CharSequence peer, CharSequence recipientPeer, CharSequence format, CharSequence channelUri, int era,
                    long length, List<Long> offsets, InputStream dataIS, OutputStream os, boolean signed)
            throws IOException, ASAPException;

    /**
     *
     * @param peer sender (optional, can be null)
     * @param recipientPeer wished recipient (optional, can be null)
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
    void assimilate(CharSequence peer, CharSequence recipientPeer, CharSequence format, CharSequence channel, int era,
                    List<Long> offsets, byte[] data, OutputStream os, boolean signed)
            throws IOException, ASAPException;


    ASAP_PDU_1_0 readPDU(InputStream is) throws IOException, ASAPException;

}
