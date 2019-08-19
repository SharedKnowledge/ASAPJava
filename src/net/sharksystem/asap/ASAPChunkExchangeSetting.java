package net.sharksystem.asap;

public class ASAPChunkExchangeSetting {
    final CharSequence owner;
    final CharSequence rootFolder;
    final CharSequence format;
    final ASAPReceivedChunkListener listener;

    public ASAPChunkExchangeSetting(CharSequence owner, CharSequence format, CharSequence rootFolder,
                                    ASAPReceivedChunkListener listener) {
        this.owner = owner;
        this.format = format;
        this.rootFolder = rootFolder;
        this.listener = listener;
    }
}
