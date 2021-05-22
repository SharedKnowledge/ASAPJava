package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPHop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ASAP_AssimilationPDU_1_0 extends ASAP_PDU_1_0 {
    String getRecipientPeer();

    byte[] getData() throws IOException;

    /**
     * @return full length of chunk data, regardless of any internal message
     * structure whatsoever.
     */
    long getLength();

    /**
     * it is assumed that the stream of bytes contains a number of
     * opaque, application specific messages. This list contains the offsets where
     * each message starts. The obvious first offset 0 is not part of this
     * list. Thus, an empty list means: There is only one application specific
     * message in this data block.
     *
     * Applications don't have to use this feature, e.g. if it tempers with
     * encryption / security issues.
     *
     * @return list of numbers. Each number states the position of the first byte
     * of each message. There is no number 0: First message starts - of course - at the beginning.
     */
    List<Integer> getMessageOffsets();

    List<ASAPHop> getASAPHopList();

    /**
     * Streams data into a storage. That method should be used instead of getData() when possible.
     * Data can directly be passed from network to its final destination without allocation memory
     * storage.
     * @param os
     * @throws IOException
     */
    void streamData(OutputStream os) throws IOException;

    /**
     * return row input stream from protocol - handle with care!
     * @return
     */
    public InputStream getInputStream() throws IOException;
}
