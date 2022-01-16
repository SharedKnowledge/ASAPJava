package net.sharksystem.asap;

import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.utils.Helper;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ASAPPeerFS extends ASAPInternalPeerWrapper implements ASAPPeerService, ASAPChunkReceivedListener {
    public static final CharSequence DEFAULT_ROOT_FOLDER_NAME = ASAPEngineFS.DEFAULT_ROOT_FOLDER_NAME;

    private final String rootFolder;
    private ASAPChunkReceivedListener chunkReceivedListener;

    public ASAPPeerFS(CharSequence owner, CharSequence rootFolder,
                      Collection<CharSequence> supportFormats) throws IOException, ASAPException {
        super.setInternalPeer(ASAPInternalPeerFS.createASAPPeer(owner, rootFolder, supportFormats, this));
        this.rootFolder = rootFolder.toString();
    }

    public void overwriteChuckReceivedListener(ASAPChunkReceivedListener listener) {
        this.chunkReceivedListener = listener;
    }

    private ASAPEncounterManager ASAPEncounterManager = null;

    @Override
    public void chunkReceived(String format, String senderE2E, String uri, int era,
                              List<ASAPHop> asapHopList) throws IOException {

        StringBuilder sb = new StringBuilder();
        String hopListString = "hoplist == null";
        if(asapHopList != null) {
            int i = 0;
            for (ASAPHop hop : asapHopList) {
                sb.append("hop#");
                sb.append(i++);
                sb.append(": ");
                sb.append(hop.toString());
                sb.append("\n");
            }
            hopListString = sb.toString();
        }

        sb = new StringBuilder();
        sb.append("\n++++++++++++++++++++++++++++++++++++++++++ chunkReceived +++++++++++++++++++++++++++++++++++++++++++\n");
        sb.append("E2E|P2P: " + senderE2E +  " | " + asapHopList.get(asapHopList.size()-1).sender() + " | uri: " + uri);
        sb.append(" | era: ");
        if(era == ASAP.TRANSIENT_ERA) sb.append("transient");
        else sb.append(era);
        sb.append(" | appFormat: " + format);
        sb.append("\n");
        sb.append(hopListString);
        sb.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        this.log(sb.toString());

        if(this.chunkReceivedListener != null) {
            this.log("chunk received listener set - call this one");
            this.chunkReceivedListener.chunkReceived(format, senderE2E, uri, era, asapHopList);
        } else {
            this.log("notify listener");
            if(this.asapMessageReceivedListenerManager.getNumberListener() > 0) {
                this.log("extract messages from chunk and notify listener");
                ASAPMessages receivedMessages =
                        Helper.getMessagesByChunkReceivedInfos(format, senderE2E, uri, this.rootFolder, era);

                this.asapMessageReceivedListenerManager.notifyReceived(
                        format, receivedMessages, true,
                        senderE2E, asapHopList);
            }

            if(this.asapChannelContentChangedListenerManager.getNumberListener() > 0) {
                this.log("notify channel content changed listener");
                this.asapChannelContentChangedListenerManager.notifyChanged(format, uri, era, true);
            }
        }
    }

    /// TODO this method makes absolutely no sense in that class.
    @Override
    public int getNumberListener() {
        return this.asapMessageReceivedListenerManager.getNumberListener();
    }

    // TODO move behaviour control into this package

    @Override
    public void sendASAPMessage(CharSequence appName, CharSequence uri, byte[] message) throws ASAPException {
        try {
            ASAPEngine engine = this.getInternalPeer().createEngineByFormat(appName);
            engine.activateOnlineMessages(this.getInternalPeer());
            engine.add(uri, message);
            // send online
            try {
                this.sendTransientASAPMessage(appName, uri, message);
            }
            catch(ASAPException e) {
                // no online peers - that's ok
                Log.writeLog(this, this.toString(), "could not send message online - that's ok.");
            }
        } catch (IOException e) {
            this.log(e.getLocalizedMessage());
            throw new ASAPException("problems getting asap engine", e);
        }
    }

    public void sendTransientASAPMessage(CharSequence appName, CharSequence uri, byte[] message)
            throws ASAPException, IOException {

        Log.writeLog(this, this.getInternalPeer().getOwner().toString(),
                "try sending transient message over existing connections ");
        this.getInternalPeer().sendTransientASAPAssimilateMessage(appName, uri, message);

    }

    public String toString() {
        return this.getInternalPeer().getOwner().toString();
    }
}
