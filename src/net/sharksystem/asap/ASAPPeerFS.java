package net.sharksystem.asap;

import net.sharksystem.asap.internals.*;
import net.sharksystem.asap.util.Helper;

import java.io.IOException;
import java.util.Collection;

public class ASAPPeerFS extends ASAPInternalPeerWrapper implements ASAPPeerService, ASAPChunkReceivedListener {
    private final String rootFolder;
    private final ASAPInternalPeer asapInternalPeer;

    public ASAPPeerFS(CharSequence owner, CharSequence rootFolder,
                      Collection<CharSequence> supportFormats) throws IOException, ASAPException {

        this.asapInternalPeer = ASAPInternalPeerFS.createASAPPeer(owner, rootFolder, supportFormats,this);
        super.setInternalPeer(asapInternalPeer);

        this.rootFolder = rootFolder.toString();
    }

    @Override
    public void chunkReceived(String format, String sender, String uri, int era) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n++++++++++++++++++++++++++++++++ chunkReceived +++++++++++++++++++++++++++++++++\n");
        sb.append("app / format: " + format + " | " + sender + " | uri: " + uri + " | era: " + era);
        sb.append("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        this.log(sb.toString());

        ASAPMessages receivedMessages =
                Helper.getMessagesByChunkReceivedInfos(format, sender, uri, this.rootFolder, era);

        this.asapMessageReceivedListenerManager.notifyReceived(format, receivedMessages, true);
    }

    @Override
    public void sendASAPMessage(CharSequence appName, CharSequence uri, byte[] message) throws ASAPException {
        try {
            ASAPEngine asapEngine = this.asapInternalPeer.createEngineByFormat(appName);
            asapEngine.activateOnlineMessages(this.getInternalPeer());
            asapEngine.add(uri, message);
        } catch (IOException e) {
            this.log(e.getLocalizedMessage());
            throw new ASAPException("problems getting asap engine", e);
        }
    }
}
