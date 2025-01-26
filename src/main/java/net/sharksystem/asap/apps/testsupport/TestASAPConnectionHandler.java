package net.sharksystem.asap.apps.testsupport;

import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPConnectionListener;
import net.sharksystem.asap.protocol.ASAPOnlineMessageSource;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class TestASAPConnectionHandler implements ASAPConnectionHandler {
    @Override
    public ASAPConnection handleConnection(InputStream is, OutputStream os, boolean encrypt, boolean sign, Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList) throws IOException, ASAPException {
        return this.dummyHandleConnection();
    }

    @Override
    public ASAPConnection handleConnection(InputStream is, OutputStream os, boolean encrypt, boolean sign, ASAPEncounterConnectionType connectionType, Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList) throws IOException, ASAPException {
        return this.dummyHandleConnection();
    }

    @Override
    public ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        return this.dummyHandleConnection();
    }

    @Override
    public ASAPConnection handleConnection(InputStream inputStream, OutputStream outputStream, ASAPEncounterConnectionType connectionType) throws IOException, ASAPException {
        return this.dummyHandleConnection();
    }

    private ASAPConnection dummyHandleConnection() {
        Log.writeLog(this, "handleConnection");
        return new ASAPConnection() {
            @Override
            public CharSequence getEncounteredPeer() {
                return "dummy";
            }

            @Override
            public void addOnlineMessageSource(ASAPOnlineMessageSource source) {

            }

            @Override
            public void removeOnlineMessageSource(ASAPOnlineMessageSource source) {

            }

            @Override
            public void addASAPConnectionListener(ASAPConnectionListener asapConnectionListener) {

            }

            @Override
            public void removeASAPConnectionListener(ASAPConnectionListener asapConnectionListener) {

            }

            @Override
            public boolean isSigned() {
                return false;
            }

            @Override
            public ASAPEncounterConnectionType getASAPEncounterConnectionType() {
                return ASAPEncounterConnectionType.UNKNOWN;
            }

            @Override
            public void kill() {

            }
        };
    }
}
