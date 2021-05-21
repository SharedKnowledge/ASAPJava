package net.sharksystem.asap;

import net.sharksystem.EncounterConnectionType;

import java.io.IOException;

public interface ASAPMessageReceivedListener {
    void asapMessagesReceived(ASAPMessages messages,
                              String senderE2E, // E2E part
                              String senderPoint2Point, boolean verified, boolean encrypted, // Point2Point part
                              EncounterConnectionType connectionType) throws IOException;
}