package net.sharksystem.asap;

public class ASAPEngineFSSetting {
    final CharSequence rootFolder;
    final CharSequence format;
    final ASAPReceivedChunkListener listener;

    public ASAPEngineFSSetting(CharSequence format, CharSequence rootFolder,
                               ASAPReceivedChunkListener listener) {
        this.format = format;
        this.rootFolder = rootFolder;
        this.listener = listener;
    }
}
