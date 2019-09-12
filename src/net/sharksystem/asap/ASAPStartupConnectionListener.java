package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAPStartupConnection;

public interface ASAPStartupConnectionListener {
    void asapStartupConnectionTerminatedWithException(ASAPStartupConnection connection, Exception terminatingException);
    void asapStartupConnectionTerminated(ASAPStartupConnection connection);
}
