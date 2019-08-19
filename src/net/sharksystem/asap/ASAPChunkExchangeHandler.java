package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_Modem_Impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ASAPChunkExchangeHandler {
    private HashMap<CharSequence, HashMap<CharSequence, FolderAndListener>> ownerMap;

    public ASAPChunkExchangeHandler(List<ASAPChunkExchangeSetting> settings) throws ASAPException {
        if(settings == null) throw new ASAPException("no settings at all - makes no sense");
        if(settings.size() == 0) throw new ASAPException("no settings - makes no sense");

        // remember settings
        this.ownerMap = new HashMap<>();

        // fill settings
        for(ASAPChunkExchangeSetting setting : settings) {
            HashMap<CharSequence, FolderAndListener> folderMap = this.ownerMap.get(setting.owner);
            if(folderMap == null) {
                folderMap = new HashMap<>();
                ownerMap.put(setting.owner, folderMap);
            }

            FolderAndListener folderAndListenerSetting = folderMap.get(setting.format);
            folderMap.put(setting.format, new FolderAndListener(setting.rootFolder, setting.listener));
        }
    }

    private FolderAndListener getFolderAndListener(CharSequence owner, CharSequence format) throws ASAPException {
        HashMap<CharSequence, FolderAndListener> folderMap = this.ownerMap.get(owner);
        if(folderMap == null) throw new ASAPException("no entry for owner: " + owner);

        FolderAndListener folderAndListener = folderMap.get(format);
        if(folderAndListener == null) throw new ASAPException("no folder for owner / format: " + owner + "/" + format);

        return folderAndListener;
    }

    CharSequence getFolder(CharSequence owner, CharSequence format) throws ASAPException {
        return this.getFolderAndListener(owner, format).folder;
    }

    ASAPReceivedChunkListener getListener(CharSequence owner, CharSequence format) throws ASAPException {
        return this.getFolderAndListener(owner, format).listener;
    }

    public void handleConnection(InputStream is, OutputStream os) {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        // issue an interest for each setting combination
        for(CharSequence owner : this.ownerMap.keySet()) {
            HashMap<CharSequence, FolderAndListener> folderMaps = this.ownerMap.get(owner);

            for(CharSequence format : folderMaps.keySet()) {
                FolderAndListener fl = folderMaps.get(format);
                // TODO hier weitermachen.

                /*
                save last seen here or in upper layers? guess it fits quite good here.
                 */
            }
        }
/*
        protocol.interest(this.owner, null, null,
                null, -1, -1, os, false);
*/


    }

    private class FolderAndListener {
        final CharSequence folder;
        final ASAPReceivedChunkListener listener;
        private ASAPEngine engine;

        FolderAndListener(CharSequence folder, ASAPReceivedChunkListener listener) {
            this.folder = folder;
            this.listener = listener;
        }

        void setASAPEngine(ASAPEngine engine) {
            this.engine = engine;
        }
    }

}
