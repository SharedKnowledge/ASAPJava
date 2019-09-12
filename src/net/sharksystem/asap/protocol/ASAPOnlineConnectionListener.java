package net.sharksystem.asap.protocol;

public interface ASAPOnlineConnectionListener {
    void asapOnlineConnectionTerminated(ASAPOnlineConnection connection, Exception terminatingException);
}
