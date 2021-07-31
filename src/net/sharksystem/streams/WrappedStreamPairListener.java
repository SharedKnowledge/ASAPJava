package net.sharksystem.streams;

public interface WrappedStreamPairListener extends StreamPairListener {
    /** data read or written
     * @param key*/
    void notifyAction(String key);
}
