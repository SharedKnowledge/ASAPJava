package net.sharksystem.asap;

import net.sharksystem.asap.utils.Helper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ASAPEncounterHelper {
    public static final String getRemoteAddress(Socket connectedSocket) throws IOException {
        InetAddress inetAddress = connectedSocket.getInetAddress();
        int port = connectedSocket.getPort();
        return inetAddress.getHostAddress() + ":" + port;
    }
}
