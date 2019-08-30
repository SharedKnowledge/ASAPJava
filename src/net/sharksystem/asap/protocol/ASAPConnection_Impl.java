package net.sharksystem.asap.protocol;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPConnection_Impl implements ASAPConnection, Runnable, ThreadFinishedListener {
    private final InputStream is;
    private final OutputStream os;
    private final ASAPConnectionListener asapConnectionListener;
    private final MultiASAPEngineFS multiASAPEngineFS;
    private final ThreadFinishedListener threadFinishedListener;
    private Thread managementThread = null;
    private final long maxExecutionTime;
    private String peer;

    public ASAPConnection_Impl(InputStream is, OutputStream os, MultiASAPEngineFS multiASAPEngineFS,
                               long maxExecutionTime, ASAPConnectionListener asapConnectionListener,
                               ThreadFinishedListener threadFinishedListener) {
        this.is = is;
        this.os = os;
        this.multiASAPEngineFS = multiASAPEngineFS;
        this.maxExecutionTime = maxExecutionTime;
        this.asapConnectionListener = asapConnectionListener;
        this.threadFinishedListener = threadFinishedListener;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    private void setPeer(String peerName) {
        if(this.peer == null) {

            this.peer = peerName;

            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("set peerName after reading first asap message: ");
            sb.append(peerName);
            System.out.println(sb.toString());

            if(this.asapConnectionListener != null) {
                this.asapConnectionListener.asapConnectionStarted(peerName, this);
            }
        }
    }

    @Override
    public CharSequence getRemotePeer() {
        return this.peer;
    }

    @Override
    public void finished(Thread t) {
        if(this.managementThread != null) {
            this.managementThread.interrupt();
        }
    }

    private void terminate(String message) {
        this.terminate(message, null);
    }

    private void terminate(String message, Exception e) {
        // write log
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append(message);
        if(e != null) {
            sb.append(e.getLocalizedMessage());
        }

        sb.append(" | ");

        try {
            this.os.close();
            this.is.close();
            sb.append("closed streams");
        }
        catch (IOException ioe) {
            sb.append("could not close streams: ");
            sb.append(ioe.getLocalizedMessage());
        }

        System.err.println(sb.toString());

        // inform listener
        if(this.asapConnectionListener != null) {
            this.asapConnectionListener.asapConnectionTerminated(e, this);
        }

        if(this.threadFinishedListener != null) {
            this.threadFinishedListener.finished(this.managementThread);
        }
    }

    public void run() {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        try {
            // let engine write their interest
            this.multiASAPEngineFS.pushInterests(this.os);
        } catch (IOException | ASAPException e) {
            this.terminate("error when pushing interest: ", e);
            return;
        }

        // start reading / processing loop
        while (true) {
            ASAPPDUReader pduReader = new ASAPPDUReader(protocol, is, this);
            pduReader.start();

            try {
                this.managementThread = Thread.currentThread();
                Thread.sleep(maxExecutionTime);
            } catch (InterruptedException e) {
                // should happen if successfully read something
            }

            // exception caught while reading?
            if (pduReader.getIoException() != null || pduReader.getAsapException() != null) {
                Exception e = pduReader.getIoException() != null ?
                        pduReader.getIoException() : pduReader.getAsapException();

                this.terminate("exception when reading from stream (stop asap session): ", e);
                return;
            }

            ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();

            if (asappdu == null) {
                this.terminate("no asap pdu read during max excecution time");
                return;
            }

            // we have read something - remember peer
            this.setPeer(asappdu.getPeer());

            // process received pdu
            try {
                Thread executor = null;

                executor = this.multiASAPEngineFS.getExecutorThread(asappdu, this.is, this.os, this);
                executor.start();

                try {
                    Thread.sleep(maxExecutionTime);
                } catch (InterruptedException e) {
                    // will hopefully happens - woke up because executor is done;
                }

                if (executor.isAlive()) {
                    // declare this a failure
                    this.terminate("process that processes asap pdu takes longer than allowed - close streams");
                }
            } catch (ASAPException e) {
                this.terminate("when executing asap received pdu", e);
            }
        }
    }
}

