package net.sharksystem.asap;

public class ASAPEngineFSSetting {
    final CharSequence folder;
    final CharSequence format;
    final ASAPReceivedChunkListener listener;

    public ASAPEngineFSSetting(CharSequence format, CharSequence folder,
                               ASAPReceivedChunkListener listener) {
        this.format = format;
        this.folder = folder;
        this.listener = listener;
    }
}
