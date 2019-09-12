package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAPOnlineConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * There is an ASAPEngine that stores its data with a filesystem.
 * One significant parameter is a root directory.
 *
 * It is good practice to use a different root for each application.
 *
 * It is also common that more than one ASAP based app is running
 * on one machine. Thus, different ASAP filesystem based engine are
 * to deal with the data depending on the ASAP format.
 *
 * That interface hides those different engines.
 */
public interface MultiASAPEngineFS {
    static final long DEFAULT_MAX_PROCESSING_TIME = 1000;

    ASAPEngine getEngineByFormat(CharSequence format) throws ASAPException, IOException;

    /**
     * get or create engine for a given application - mainly means: setup folder
     * @param appName
     * @return
     */
    ASAPEngine getASAPEngine(CharSequence appName, CharSequence format) throws IOException, ASAPException;

    /**
     * handle that newly established connection to another ASAP peer
     * @param is
     * @param os
     * @throws IOException
     * @throws ASAPException
     */
    void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;

    void pushInterests(OutputStream os) throws IOException, ASAPException;

    boolean existASAPConnection(CharSequence recipient);

    ASAPOnlineConnection getASAPOnlineConnection(CharSequence recipient);

    CharSequence getOwner();

    void newEra() throws IOException, ASAPException;

    void setASAPChunkReceivedListener(CharSequence appName, ASAPChunkReceivedListener listener) throws ASAPException;

    ASAPChunkReceivedListener getListenerByFormat(CharSequence format) throws ASAPException;

    /**
     * if on = true: a connection is kept open as long as possible to support online exchange
     * default: false
     * @param on
     */
    void setSupportOnline(boolean on);
}
