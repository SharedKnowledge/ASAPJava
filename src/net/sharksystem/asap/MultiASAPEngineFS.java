package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPConnectionListener;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;
import net.sharksystem.asap.protocol.ThreadFinishedListener;

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
    public static final long DEFAULT_MAX_PROCESSING_TIME = 1000;

    public ASAPEngine getEngineByFormat(CharSequence format) throws ASAPException, IOException;

    /**
     * handle that newly established connection to another ASAP peer
     * @param is
     * @param os
     * @throws IOException
     * @throws ASAPException
     */
    public ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;

    public void pushInterests(OutputStream os) throws IOException, ASAPException;

    Thread getExecutorThread(ASAP_PDU_1_0 asappdu, InputStream is, OutputStream os,
                             ThreadFinishedListener threadFinishedListener) throws ASAPException;

    boolean existASAPConnection(CharSequence recipient);

    ASAPConnection getASAPConnection(CharSequence recipient);

    CharSequence getOwner();

    void newEra() throws IOException, ASAPException;
}
