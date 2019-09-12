package net.sharksystem.asap.protocol;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPChunkExchangeSession extends ASAPSession implements ASAPStartupConnection, ThreadFinishedListener {
    private final ASAPStartupConnectionListener asapStartupConnectionListener;
    private final ThreadFinishedListener threadFinishedListener;
    private final long maxExecutionTime;
    private String peer;

    public ASAPChunkExchangeSession(InputStream is, OutputStream os,
                                    MultiASAPEngineFS multiASAPEngineFS,
                                    ASAP_1_0 protocol, long maxExecutionTime,
                                    ASAPStartupConnectionListener asapStartupConnectionListener,
                                    ThreadFinishedListener threadFinishedListener) {

        super(protocol, multiASAPEngineFS, is, os);

        this.maxExecutionTime = maxExecutionTime;
        this.asapStartupConnectionListener = asapStartupConnectionListener;
        this.threadFinishedListener = threadFinishedListener;
    }

    private String getLogStart() {
        if(this.peer != null) {
            return this.getClass().getSimpleName() + "(connected with: " + this.peer + "): ";
        }

        return this.getClass().getSimpleName() + "(not connected): ";
    }

    private void setPeer(String peerName) {
        if(this.peer == null) {

            this.peer = peerName;

            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("set peerName after reading first asap message: ");
            sb.append(peerName);
            System.out.println(sb.toString());
        }
    }

    @Override
    public boolean streamsOpen() {
        return false;
    }

    @Override
    public CharSequence getPeerID() throws ASAPException {
        if(this.peer == null) {
            throw new ASAPException("have not got peer id yet");
        }

        return this.peer;
    }

    @Override
    public InputStream getInputStream() throws ASAPException {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws ASAPException {
        return null;
    }

    private void finishSession(String message) {
        this.finishSession(message, null);
    }

    private void finishSession(String message, Exception e) {
        // write log
        StringBuilder sb = this.startLog();
        sb.append(message);
        if(e != null) {
            sb.append(e.getLocalizedMessage());
        }

        System.out.println(sb.toString());

        // inform listener
        if(this.asapStartupConnectionListener != null) {
            if(e != null) {
                this.asapStartupConnectionListener.asapStartupConnectionTerminatedWithException(this, e);
            } else {
                this.asapStartupConnectionListener.asapStartupConnectionTerminated(this);
            }
        }

        if(this.threadFinishedListener != null) {
            this.threadFinishedListener.finished(this);
        }
    }

    public void run() {
        try {
            // let engine write their interest
            this.multiASAPEngineFS.pushInterests(this.os);
        } catch (IOException | ASAPException e) {
            this.finishSession("error when pushing interests: ", e);
            return;
        }

        // start reading / processing loop
        while (true) {
            ASAP_PDU_1_0 asappdu = null;
            try {
                asappdu = this.readASAPPDU(this.maxExecutionTime);
                // we have read something - remember peer
                this.setPeer(asappdu.getPeer());
                // process received pdu - could force reading - keep it separated from reader.
                this.runASAPExecutor(asappdu, this.maxExecutionTime);
            }
            catch(ASAPExecTimeExceededException e) {
                this.finishSession("nothing to read or execute - ok");
                return;
            } catch (ASAPException | IOException e) {
                this.finishSession("pdu reading: ", e);
                return;
            }
        } // next loop - read pdu
    }

    private StringBuilder startLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        return sb;
    }
}
