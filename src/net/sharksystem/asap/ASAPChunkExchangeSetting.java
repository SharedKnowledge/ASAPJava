package net.sharksystem.asap;

public class ASAPChunkExchangeSetting {
    final CharSequence rootFolder;
    final CharSequence format;
    final ASAPReceivedChunkListener listener;

    public ASAPChunkExchangeSetting(CharSequence format, CharSequence rootFolder,
                                    ASAPReceivedChunkListener listener) {
        this.format = format;
        this.rootFolder = rootFolder;
        this.listener = listener;
    }
}
