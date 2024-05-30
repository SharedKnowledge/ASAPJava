package net.sharksystem.asap.cmdline;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.engine.ASAPChunkAssimilatedListener;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                 ASAP API: callbacks ASAPChunkReceived                                  //
////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class ExampleASAPChunkReceivedListener implements ASAPChunkAssimilatedListener {
    private final String rootFolder;
    private List<ASAPChunkReceivedParameters> receivedList = new ArrayList<>();

    public ExampleASAPChunkReceivedListener(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    @Override
    public void chunkStored(String format, String senderE2E, String uri, int era,
                            List<ASAPHop> asapHop) throws IOException {

        this.receivedList.add(new ASAPChunkReceivedParameters(format, senderE2E, uri, era));
    }

    @Override
    public void transientMessagesReceived(ASAPMessages transientMessages, ASAPHop asapHop) throws IOException {
        Log.writeLog(this, "transient message received - TODO?");
    }

    public List<ASAPChunkReceivedParameters> getReceivedList() { return this.receivedList; }

    public class ASAPChunkReceivedParameters {
        private final String format;
        private final String sender;
        private final String uri;
        private final int era;

        private ASAPChunkReceivedParameters(String format, String sender, String uri, int era) {
            Log.writeLog(this, "ASAPChunkReceivedParameters: chunk received: " + format + " | " + sender + " | " + uri);
            this.format = format;
            this.sender = sender;
            this.uri = uri;
            this.era = era;
        }

        public String getFormat() { return this.format; }
        public String getSender() { return this.sender; }
        public String getUri() { return this.uri; }
        public int getEra() { return this.era; }
    }
}
