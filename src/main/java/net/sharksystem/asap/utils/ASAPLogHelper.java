package net.sharksystem.asap.utils;

import net.sharksystem.utils.Log;
import net.sharksystem.utils.Utils;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;

import java.io.*;

public class ASAPLogHelper {
    public static final String SERIALIZATION_DELIMITER = "|||";

    public static ASAPMessages getMessagesByChunkReceivedInfos(String format, String sender, String uri,
                                                               String folderName, int era) {
        try {
            String rootIncomingStorage = folderName + "/" + Utils.url2FileName(format);
            Log.writeLog(ASAPLogHelper.class, "try getting storage in folder " + rootIncomingStorage);
            ASAPEngine existingASAPEngineFS =
                    ASAPEngineFS.getExistingASAPEngineFS(rootIncomingStorage);
            Log.writeLog(ASAPLogHelper.class, "got existing asap engine");

            ASAPChunkStorage chunkStorage = existingASAPEngineFS.getReceivedChunksStorage(sender);
            Log.writeLog(ASAPLogHelper.class, "got incoming channel of " + sender);

            ASAPMessages asapMessages = chunkStorage.getASAPMessages(uri, era, era);
            Log.writeLog(ASAPLogHelper.class, "got messages uri: " + uri + " / era: " + era);

            return asapMessages;
        } catch (IOException | ASAPException e) {
            Log.writeLog(ASAPLogHelper.class, "could not access message after be informed about new chunk arrival"
                            + e.getLocalizedMessage());
        }

        return null;
    }
}
