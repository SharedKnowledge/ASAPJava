package net.sharksystem.asap.engine;

public class ASAPEngineFSSetting {
    final CharSequence folder;
    final CharSequence format;
    final ASAPChunkReceivedListener listener;

    public ASAPEngineFSSetting(CharSequence format, CharSequence folder,
                               ASAPChunkReceivedListener listener) {
        this.format = format;
        this.folder = folder;
        this.listener = listener;
    }
}
