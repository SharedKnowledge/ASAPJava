package net.sharksystem.asap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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
    public void handleConnection(InputStream is, OutputStream os)  throws IOException, ASAPException;
}
