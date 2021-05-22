package net.sharksystem.asap;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * An ASAP chunk is a set of message with the same format, same uri and same era.
 *
 * Usually, there will be a running ASAP protocol engine that writes received messages into
 * chunks and delivers chunks to peers during an encounter
 *
 * @author thsc
 */
public interface ASAPChunk {
    /**
     *
     * @return number of message in that chunk
     */
    int getNumberMessage();

    /**
     * URI of all messages within that chunk
     * @return
     * @throws IOException
     */
    String getUri() throws IOException;

    /**
     *
     * @return iterator of all messages in the chunk
     * @throws IOException
     */
    Iterator<byte[]> getMessages() throws IOException;

    /**
     * remove that chunk.. drop all object references after
     * calling this methods. Further calls on this object
     * have an undefined behaviour.
     */
    public void drop();

    /**
     *
     * @return era all messages in that chunk
     * @throws IOException
     */
    public int getEra() throws IOException;

    /**
     * Handle this methode with great care.
     * It will most probable disappear of we find time to re-implement the PKI projects. Until then:
     * <br/><br/>
     * A chunk behaves like an in- or outbox in an mail system. Sometimes a chunk is both.
     * An ASAP peer offers a send message which is the preferred and recommended way to send messages.
     * <br/><br/>
     * A peer behaves pretty simple internally. A message that is ought to be sent is stored in
     * a chunk with current era. Received messages are stored in chunks as well. There is no need
     * to change those chunks from any application.
     * <br/><br/>
     * Peers can also be asked to re-deliver received messages. In that case, an application could think
     * of managing such chunks to drop message (usually whole chunks) which are not meant to be
     * re-delivered. Actually, it is not a really good idea. But we did. And we haven't not yet found time
     * to take it out. And we need a running public key infrastructure.
     * <br/><br/>
     * Furthermore, an ASAP peer keeps a list of its encounters. It can figure out the last era
     * in which another peer was met. If they have never met before the initial era would be taken in
     * its place.
     * <br/><br/>
     * Now, default behaviour of a peer is to transmit all messages from this last-met-era
     * until current era.
     * <br/><br/>
     * Sometimes, it seemed to be efficient to manipulate those chunks to slightly change the set
     * of delivered messages. I our decentralized PKI, we decided to implement the certificate
     * storage as decorator of a chunk storage. That's actually quite efficient but requires
     * a lot of explaining and hinders us to change the internal structure.
     * <br/><br/>
     * Unfortunately, it forced us to make this feature public. And here we are. You can add a message
     * to a chunk. You should not do it until you are absolutely sure what you are doing.
     * It is something of a hack. Efficient yet but not good.
     *
     * @param messageAsBytes
     * @throws IOException
     * @see net.sharksystem.asap.ASAPPeer#sendASAPMessage(CharSequence, CharSequence, byte[])
     */
    void addMessage(byte[] messageAsBytes) throws IOException;

    List<ASAPHop> getASAPHopList();

    void setASAPHopList(List<ASAPHop> asapHopList) throws IOException;
}
