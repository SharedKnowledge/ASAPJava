package net.sharksystem.asap;

public interface ASAPMessageCompare {
    /**
     * Returns if message A is earlier than message B. This interface must be implemented
     * by an applications and is parameter to retrieve a messages iterator object. If set, this
     * comparision would define the order on which those messages are delivered.
     * <br/><br/>
     * Note: It is always assumed that message sent / received in an earlier era are always earlier.
     *
     * @param messageA
     * @param messageB
     * @return
     */
    boolean earlier(byte[] messageA, byte[] messageB);
}
