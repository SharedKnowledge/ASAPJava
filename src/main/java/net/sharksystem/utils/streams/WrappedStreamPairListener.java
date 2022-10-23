package net.sharksystem.utils.streams;

public interface WrappedStreamPairListener extends StreamPairListener {
    /** data read or written
     * @param key*/
    void notifyAction(String key);
}
