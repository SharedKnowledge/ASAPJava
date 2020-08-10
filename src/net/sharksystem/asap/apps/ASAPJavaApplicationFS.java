package net.sharksystem.asap.apps;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class ASAPJavaApplicationFS implements ASAPJavaApplication {
    private ASAPPeer multiEngine;
    private final CharSequence owner;
    private final CharSequence rootFolder;

    public static ASAPJavaApplication createASAPJavaApplication(CharSequence owner, CharSequence rootFolder,
                                                                Collection<CharSequence> supportedFormats)
            throws IOException, ASAPException {

        return new ASAPJavaApplicationFS(owner, rootFolder, supportedFormats);
    }

    private ASAPJavaApplicationFS(CharSequence owner, CharSequence rootFolder, Collection<CharSequence> supportedFormats)
            throws IOException, ASAPException {

        this.owner = owner;
        this.rootFolder = rootFolder;
        this.multiEngine = this.getMulitEngine();

        if(supportedFormats != null && !supportedFormats.isEmpty()) {
            // ensure that supported format engine are up and running
            for(CharSequence format : supportedFormats) {
                this.multiEngine.createEngineByFormat(format);

                // setup inter era message delivery
                System.out.println(this.getLogStart() + "setup inter era message exchange for " + format);
                ASAPEngine engine = multiEngine.getEngineByFormat(format);
                new ASAPSingleProcessOnlineMessageSender(this.multiEngine, engine);
            }
        }
    }

    private ASAPPeer getMulitEngine() throws IOException, ASAPException {
        // TODO: re-create any time - keep track of potential changes in external storage (file system)?
        ASAPPeer multiEngine = ASAPPeerFS.createASAPPeer(owner, rootFolder,
                ASAPPeer.DEFAULT_MAX_PROCESSING_TIME, null);

        for(CharSequence format : this.messageReceivedListener.keySet()) {
            ASAPMessageReceivedListener listener = this.messageReceivedListener.get(format);
            multiEngine.setASAPChunkReceivedListener(format, new MessageChunkReceivedListenerWrapper(listener));
        }

        return multiEngine;
    }

    @Override
    public void sendASAPMessage(CharSequence format, CharSequence uri, Collection<CharSequence> recipients,
                                byte[] message) throws ASAPException, IOException {

        ASAPEngine engine = this.getMulitEngine().getEngineByFormat(format);

        engine.createChannel(uri, recipients);
        engine.add(uri, message);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    private Map<CharSequence, ASAPMessageReceivedListener> messageReceivedListener = new HashMap<>();

    @Override
    public void setASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener)
            throws ASAPException, IOException {

        // wrap receiver and add listener to multiengine
        this.getMulitEngine().setASAPChunkReceivedListener(format, new MessageChunkReceivedListenerWrapper(listener));

        // set with multi engine
        this.messageReceivedListener.put(format, listener);
    }

    @Override
    public void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        this.getMulitEngine().handleConnection(is, os);
    }

    private class MessageChunkReceivedListenerWrapper implements ASAPChunkReceivedListener {
        private final ASAPMessageReceivedListener listener;

        public MessageChunkReceivedListenerWrapper(ASAPMessageReceivedListener listener) throws ASAPException {
            if(listener == null) throw new ASAPException("listener must not be null");
            this.listener = listener;
        }

        @Override
        public void chunkReceived(String format, String sender, String uri, int era) {
            System.out.println(getLogStart() + "chunk received - convert to asap message received");
            try {
                ASAPEngine engine = ASAPJavaApplicationFS.this.multiEngine.getEngineByFormat(format);
                ASAPMessages messages = engine.getReceivedChunksStorage(sender).getASAPMessages(uri, era);
                this.listener.asapMessagesReceived(messages);
            } catch (ASAPException | IOException e) {
                System.out.println(getLogStart() + e.getLocalizedMessage());
            }
        }
    }
}
