package net.sharksystem.asap.apps;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface ASAPJavaApplication {
    /**
     * send an asap message - as soon as possible - to all recipients. If recipients are set null - message is delivered
     * to any peer whatsoever.
     * @param format
     * @param uri
     * @param recipients white list of recipients. If null - means anybody
     * @param message
     * @throws ASAPException e.g. format no supported
     */
    void sendASAPMessage(CharSequence format, CharSequence uri, Collection<CharSequence> recipients, byte[] message)
            throws ASAPException, IOException;

    /**
     * add listener for incomming messages for a given format
     * @param format
     * @param listener
     * @throws ASAPException format not supported
     */
    void setASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) throws ASAPException, IOException;

    /**
     * Run an asap session with that those streams
     * @param is
     * @param os
     * @throws IOException
     * @throws ASAPException
     */
    void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;
}
