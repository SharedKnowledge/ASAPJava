package net.sharksystem.asap.protocol;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPChunkExchangeSession extends Thread implements ASAPStartupConnection, ThreadFinishedListener {
    private final InputStream is;
    private final OutputStream os;
    private final ASAPStartupConnectionListener asapStartupConnectionListener;
    private final MultiASAPEngineFS multiASAPEngineFS;
    private final ThreadFinishedListener threadFinishedListener;
    private final ASAP_1_0 protocol;
    private Thread managementThread = null;
    private final long maxExecutionTime;
    private String peer;

    private Thread thread2wait4;

    public ASAPChunkExchangeSession(InputStream is, OutputStream os,
                                    MultiASAPEngineFS multiASAPEngineFS,
                                    ASAP_1_0 protocol, long maxExecutionTime,
                                    ASAPStartupConnectionListener asapStartupConnectionListener,
                                    ThreadFinishedListener threadFinishedListener) {
        this.is = is;
        this.os = os;
        this.multiASAPEngineFS = multiASAPEngineFS;
        this.protocol = protocol;
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

    @Override
    public void finished(Thread t) {
        if(t == this.thread2wait4) {
            this.thread2wait4 = null;
        }

        if(this.managementThread != null) {
            this.managementThread.interrupt();
        }
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
            this.threadFinishedListener.finished(this.managementThread);
        }
    }

    public void run() {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        try {
            // let engine write their interest
            this.multiASAPEngineFS.pushInterests(this.os);
        } catch (IOException | ASAPException e) {
            this.finishSession("error when pushing interests: ", e);
            return;
        }

        // start reading / processing loop
        while (true) {
            // read asap pdu
            ASAPPDUReader pduReader = new ASAPPDUReader(protocol, is, this);
            this.thread2wait4 = pduReader;
            pduReader.start();

            // send online messages would be much better here
            if(this.thread2wait4 != null) {
                // wait for reader
                try {
                    this.managementThread = Thread.currentThread();
                    Thread.sleep(maxExecutionTime);
                } catch (InterruptedException e) {
                    // should happen if successfully read something
                }
            }

            // exception caught while reading?
            if (pduReader.getIoException() != null || pduReader.getAsapException() != null) {
                Exception e = pduReader.getIoException() != null ?
                        pduReader.getIoException() : pduReader.getAsapException();

                this.finishSession("exception when reading from stream (stop asap session): ", e);
                return;
            }

            ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();

            if (asappdu == null) {
                this.finishSession("no pdu, no exception, give up");
                return;
            } else {
                // we have read something - remember peer
                this.setPeer(asappdu.getPeer());

                // process received pdu
                try {
                    ASAPEngine engine = this.multiASAPEngineFS.getEngineByFormat(asappdu.getFormat());
                    ASAPChunkReceivedListener listener = this.multiASAPEngineFS.getListenerByFormat(asappdu.getFormat());

                    Thread executor = new ASAPPDUExecutor(asappdu, this.is, this.os,
                            engine, new ASAP_Modem_Impl(),
                            listener, // chunk received listener (can be null)
                            this);

                    executor.start();

                    boolean threadFinished = false;
                    try {
                        Thread.sleep(maxExecutionTime);
                    } catch (InterruptedException e) {
                        threadFinished = true;
                        // will hopefully happens - woke up because executor is done;
                    }

                    if (!threadFinished) {
                        // declare this a failure
                        this.finishSession("processing asap pdu takes longer than allowed - close streams");
                        return;
                    }

                } catch (ASAPException | IOException e) {
                    this.finishSession("when executing asap received pdu: ", e);
                    return;
                }
            } // asap pdu != null
        } // next loop - read pdu
    }

    protected void runASAPExecutor(ASAP_PDU_1_0 asapPDU) {

    }

    private StringBuilder startLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        return sb;
    }
}
