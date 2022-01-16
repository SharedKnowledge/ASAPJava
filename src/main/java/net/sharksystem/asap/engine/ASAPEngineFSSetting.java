package net.sharksystem.asap.engine;

public class ASAPEngineFSSetting {
    final CharSequence folder;
    final CharSequence format;
    final ASAPChunkAssimilatedListener listener;

    public ASAPEngineFSSetting(CharSequence format, CharSequence folder,
                               ASAPChunkAssimilatedListener listener) {
        this.format = format;
        this.folder = folder;
        this.listener = listener;
    }
}
