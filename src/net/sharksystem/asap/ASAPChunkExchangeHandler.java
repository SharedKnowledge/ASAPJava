package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_Interest_PDU_1_0;
import net.sharksystem.asap.protocol.ASAP_Modem_Impl;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

public class ASAPChunkExchangeHandler {
    private final CharSequence owner;
    private final HashMap<CharSequence, FormatSettings> folderMap;

    public ASAPChunkExchangeHandler(CharSequence owner, List<ASAPChunkExchangeSetting> settings) throws ASAPException {
        if(settings == null) throw new ASAPException("no settings at all - makes no sense");
        if(settings.size() == 0) throw new ASAPException("no settings - makes no sense");

        this.owner = owner;
        this.folderMap = new HashMap<>();

        // fill settings
        for(ASAPChunkExchangeSetting setting : settings) {
            folderMap.put(setting.format, new FormatSettings(setting.rootFolder, setting.listener));
        }
    }

    private FormatSettings getFormatSettings(CharSequence format) throws ASAPException {
        FormatSettings folderAndListener = folderMap.get(format);
        if(folderAndListener == null) throw new ASAPException("no folder for owner / format: " + owner + "/" + format);

        return folderAndListener;
    }

    public void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        // issue an interest for each owner / format combination
        for(CharSequence format : this.folderMap.keySet()) {
            protocol.interest(this.owner, null, format,null, -1, -1, os, false);
        }

        // read pdu from the other side from the other side
        ASAP_PDU_1_0 asapPDU = protocol.readPDU(is);

        // get engine
        FormatSettings formatSettings = this.getFormatSettings(asapPDU.getFormat());
        if(formatSettings.engine == null) {
            ASAPEngine asapEngine = ASAPEngineFS.getASAPEngine(
                                        owner.toString(),
                                        formatSettings.folder.toString(),
                                        asapPDU.getFormat());

            formatSettings.setASAPEngine(asapEngine);
        }

        // TODO HIER WEITERMACHEN
        if(asapPDU.getCommand() == ASAP_1_0.INTEREST_CMD) {
            Thread thread = formatSettings.engine.handleASAPInterest(
                    (ASAP_Interest_PDU_1_0) asapPDU,
                    protocol, is, os,
                    formatSettings.listener);
        }
    }

    private class FormatSettings {
        final CharSequence folder;
        final ASAPReceivedChunkListener listener;
        private ASAPEngine engine;

        FormatSettings(CharSequence folder, ASAPReceivedChunkListener listener) {
            this.folder = folder;
            this.listener = listener;
        }

        void setASAPEngine(ASAPEngine engine) {
            this.engine = engine;
        }
    }

}
