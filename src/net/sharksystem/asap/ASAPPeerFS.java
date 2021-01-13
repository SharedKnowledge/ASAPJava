package net.sharksystem.asap;

import net.sharksystem.asap.internals.*;
import net.sharksystem.asap.util.Helper;

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
    public void chunkReceived(String format, String sender, String uri, int era) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n++++++++++++++++++++++++++++++++ chunkReceived +++++++++++++++++++++++++++++++++\n");
        sb.append("app / format: " + format + " | " + sender + " | uri: " + uri + " | era: " + era);
        sb.append("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        this.log(sb.toString());

        if(this.chunkReceivedListener != null) {
            this.log("chunk received listener set - call this one");
            this.chunkReceivedListener.chunkReceived(format, sender, uri, era);
        } else {
            this.log("extract messages from chunk and notify listener");
            ASAPMessages receivedMessages =
                    Helper.getMessagesByChunkReceivedInfos(format, sender, uri, this.rootFolder, era);

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
                this.getInternalPeer().sendOnlineASAPAssimilateMessage(appName, uri, message);
            }
            catch(ASAPException e) {
                // no online peers - that's ok
                this.log("could not send message online - that's ok.");
            }
        } catch (IOException e) {
            this.log(e.getLocalizedMessage());
            throw new ASAPException("problems getting asap engine", e);
        }
    }
}
