package net.sharksystem.asap;

import net.sharksystem.SharkException;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.utils.ASAPLogHelper;
import net.sharksystem.fs.ExtraData;
import net.sharksystem.utils.Log;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ASAPPeerFS extends ASAPInternalPeerWrapper implements ASAPPeerService, ASAPChunkAssimilatedListener {
    public static final CharSequence DEFAULT_ROOT_FOLDER_NAME = ASAPEngineFS.DEFAULT_ROOT_FOLDER_NAME;

    private final String rootFolder;
    private ASAPChunkAssimilatedListener chunkReceivedListener;

    public ASAPPeerFS(CharSequence owner, CharSequence rootFolder,
                      Collection<CharSequence> supportFormats) throws IOException, ASAPException {
        super.setInternalPeer(ASAPInternalPeerFS.createASAPPeer(owner, rootFolder, supportFormats, this));
        this.rootFolder = rootFolder.toString();
    }

    public ASAPPeerFS(CharSequence owner, CharSequence rootFolder) throws IOException, ASAPException {
        super.setInternalPeer(ASAPInternalPeerFS.createASAPPeer(owner, rootFolder, null, this));
        this.rootFolder = rootFolder.toString();
    }

    public void overwriteChuckReceivedListener(ASAPChunkAssimilatedListener listener) {
        Log.writeLogErr(this, this.getPeerID(), "do not use chunk received listener - message received listener is better");
        this.chunkReceivedListener = listener;
    }

    private void chunkAssimilated(ASAPMessages receivedMessages, CharSequence format,
                                  CharSequence senderE2E, CharSequence uri, int era,
                                  List<ASAPHop> asapHopList, boolean callListener) {
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
        sb.append("\n+++++++++++++++++++++++++++++++++++++++ chunkReceived +++++++++++++++++++++++++++++++++++++++++\n");
        sb.append("E2E|P2P: " + senderE2E +  " | " + asapHopList.get(asapHopList.size()-1).sender() + " | uri: " + uri);
        sb.append(" | era: ");
        if(era == ASAP.TRANSIENT_ERA) sb.append("transient");
        else sb.append(era);
        sb.append(" | appFormat: " + format);
        sb.append("\n");
        sb.append(hopListString);
        sb.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Log.writeLog(this, this.getPeerID(), sb.toString());

        if(callListener) {
            // call listener
            String numberListeners = "null";
            if(this.asapMessageReceivedListenerManager != null) {
                numberListeners = String.valueOf(this.asapMessageReceivedListenerManager.getNumberListener());
            }

            Log.writeLog(this, this.getPeerID(), "notify listeners; number: " + numberListeners);
            if (this.asapMessageReceivedListenerManager.getNumberListener() > 0) {
                this.asapMessageReceivedListenerManager.notifyReceived(
                        format, receivedMessages, true,
                        senderE2E.toString(), asapHopList);
            }

            if (this.asapChannelContentChangedListenerManager.getNumberListener() > 0) {
                Log.writeLog(this, this.getPeerID(),"notify channel content changed listener");
                this.asapChannelContentChangedListenerManager.notifyChanged(format, uri, era, true);
            }
        }
    }

    @Override
    public void transientMessagesReceived(ASAPMessages transientMessages, ASAPHop asapHop) throws IOException {
        // produce hop list object

        List<ASAPHop> asapHopList = new ArrayList<>();
        asapHopList.add(asapHop);
        this.chunkAssimilated(transientMessages,
                transientMessages.getFormat(), asapHop.sender(), transientMessages.getURI(),
                ASAP.TRANSIENT_ERA, asapHopList, true);
    }

    @Override
    public void chunkStored(String format, String senderE2E, String uri, int era,
                            List<ASAPHop> asapHopList) throws IOException {
        if(this.chunkReceivedListener != null) {
            Log.writeLog(this, this.getPeerID(),"chunk received listener set - call this one");
            this.chunkReceivedListener.chunkStored(format, senderE2E, uri, era, asapHopList);
            this.chunkAssimilated(null, format, senderE2E, uri, era, asapHopList, false);
        } else {
            Log.writeLog(this, this.getPeerID(),"extract messages from chunk and notify listener");
            ASAPMessages receivedMessages =
                    ASAPLogHelper.getMessagesByChunkReceivedInfos(format, senderE2E, uri, this.rootFolder, era);

            this.chunkAssimilated(receivedMessages, format, senderE2E, uri, era, asapHopList, true);
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
            // send online (already done by activating online messages and adding message with engine
            /*
            try {
                this.getInternalPeer().sendOnlineASAPAssimilateMessage(appName, uri, engine.getEra(), message);
            }
            catch(ASAPException e) {
                // no online peers - that's ok
                Log.writeLog(this, this.toString(), "could not send message online - that's ok.");
            }
             */
        } catch (IOException e) {
            Log.writeLog(this, this.getPeerID(),e.getLocalizedMessage());
            throw new ASAPException("problems getting asap engine", e);
        }
    }

    public void sendTransientASAPMessage(CharSequence appName, CharSequence uri, byte[] message)
            throws ASAPException, IOException {

        Log.writeLog(this, this.getInternalPeer().getOwner().toString(),
                "try sending transient message over existing connections ");
        this.getInternalPeer().sendTransientASAPAssimilateMessage(appName, uri, message);

    }

    public void sendTransientASAPMessage(CharSequence nextHopPeerID,
                 CharSequence appName, CharSequence uri, byte[] message) throws ASAPException, IOException {

        Log.writeLog(this, this.getInternalPeer().getOwner().toString(),
                "try sending transient message over existing connections to peerID: " + nextHopPeerID);
        this.getInternalPeer().sendTransientASAPAssimilateMessage(appName, uri, nextHopPeerID, message);
    }

    public String toString() {
        return this.getInternalPeer().getOwner().toString();
    }

    @Override
    public ExtraData getExtraData() throws SharkException, IOException {
        return this.getInternalPeer().getExtraData();
    }
}
