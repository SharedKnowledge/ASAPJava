package net.sharksystem.asap;

import net.sharksystem.EncounterConnectionType;
import net.sharksystem.asap.engine.*;
import net.sharksystem.asap.utils.Helper;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.Collection;

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

    @Override
    public void chunkReceived(String format, String senderE2E, String uri, int era,
                              String senderPoint2Point, boolean verified, boolean encrypted,
                              EncounterConnectionType connectionType) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("\n++++++++++++++++++++++++++++++++++++++++++ chunkReceived +++++++++++++++++++++++++++++++++++++++++++\n");
        sb.append("E2E|P2P: " + senderE2E +  " | " + senderPoint2Point + " | uri: " + uri + " | era: " + era + " | appFormat: " + format);
        sb.append("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        this.log(sb.toString());

        if(this.chunkReceivedListener != null) {
            this.log("chunk received listener set - call this one");
            this.chunkReceivedListener.chunkReceived(format, senderE2E, uri, era, senderPoint2Point,
                    verified, encrypted, connectionType);
        } else {
            this.log("extract messages from chunk and notify listener");
            ASAPMessages receivedMessages =
                    Helper.getMessagesByChunkReceivedInfos(format, senderE2E, uri, this.rootFolder, era);

            this.asapMessageReceivedListenerManager.notifyReceived(format, receivedMessages, true);
        }
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
                this.sendOnlineASAPMessage(appName, uri, message);
            }
            catch(ASAPException e) {
                // no online peers - that's ok
                Log.writeLog(this, "(" + this.getInternalPeer().getOwner()
                        + "could not send message online - that's ok.");
            }
        } catch (IOException e) {
            this.log(e.getLocalizedMessage());
            throw new ASAPException("problems getting asap engine", e);
        }
    }

    public void sendOnlineASAPMessage(CharSequence appName, CharSequence uri, byte[] message)
            throws ASAPException, IOException {

        Log.writeLog(this, this.getInternalPeer().getOwner().toString(),
                "try sending message over existing connections ");
        this.getInternalPeer().sendOnlineASAPAssimilateMessage(appName, uri, message);

    }

}
