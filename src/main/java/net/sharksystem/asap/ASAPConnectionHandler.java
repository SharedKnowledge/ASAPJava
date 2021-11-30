package net.sharksystem.asap;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.crypto.ASAPPoint2PointCryptoSettings;
import net.sharksystem.asap.protocol.ASAPConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public interface ASAPConnectionHandler {
    /**
     * Ask the entity to run an ASAP session based on this point-to-point connection.
     * @param is stream to read from
     * @param os stream to write into
     * @param encrypt encrypt this point-to-point communication
     * @param sign sign this point-to-point communication
     * @param appsWhiteList if not null - only message from this app (synonym: with this format)
     *                      within that list are handled. All other messages are ignored
     * @param appsBlackList if not null - message from this app (synonym: with this format)
     *                      are not handled. Overwrites white list entries.
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    ASAPConnection handleConnection(
            InputStream is, OutputStream os, boolean encrypt, boolean sign,
            Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList
    ) throws IOException, ASAPException;

    ASAPConnection handleConnection(
            InputStream is, OutputStream os, boolean encrypt, boolean sign,EncounterConnectionType connectionType,
            Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList
    ) throws IOException, ASAPException;

    /**
     *
     * Ask the entity to run an ASAP session based on this point-to-point connection. Message are neither
     * encrypted nor signed. All formats are accepted in principle.
     * @param is stream to read from
     * @param os stream to write into
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;

    ASAPConnection handleConnection(InputStream inputStream, OutputStream outputStream,
                                    EncounterConnectionType connectionType) throws IOException, ASAPException;
}
