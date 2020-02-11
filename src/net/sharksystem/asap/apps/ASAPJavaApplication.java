package net.sharksystem.asap.apps;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * This interface offers methods to build an ASAP based application. ASAP is a store and forward protocol tht is
 * specifically created to support Ad-hoc network communication. ASAP messages can be annotated with two parameters:
 * format an uri. A 1-n relationship is assumed between format and a processing application. Each format is assumed to be
 * processed by a single application that can subscribe as listener to get informed about newly arrived messages.
 *
 * Developers are free of course to write applications that support more than one format.
 *
 * The uri describes something within an application domain. URI structure is applications specific.
 *
 * It is strongly recommended to use only one ASAP application on each device. Do <b>not</b> derive from this interfaces
 * and implement apps. Implement listeners instead and issue messages over this interface.
 */
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
