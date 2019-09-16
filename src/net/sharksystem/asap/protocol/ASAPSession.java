package net.sharksystem.asap.protocol;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ASAPSession extends Thread implements ThreadFinishedListener {
    protected final ASAP_1_0 protocol;
    protected final MultiASAPEngineFS multiASAPEngineFS;
    protected final InputStream is;
    protected final OutputStream os;

    private Thread thread2wait4;
    private Thread managementThread = null;

    ASAPSession(ASAP_1_0 protocol, MultiASAPEngineFS multiASAPEngineFS, InputStream is, OutputStream os) {
        this.protocol = protocol;
        this.multiASAPEngineFS = multiASAPEngineFS;
        this.is = is;
        this.os = os;
    }

    private void runObservedThread(Thread t, long maxExecutionTime) throws ASAPExecTimeExceededException {
        this.thread2wait4 = t;
        t.start();

        // wait for reader
        try {
            this.managementThread = Thread.currentThread();
            Thread.sleep(maxExecutionTime);
        } catch (InterruptedException e) {
            // was woken up by thread - that's good
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("thread (");
        sb.append(t.getClass().getSimpleName());
        sb.append(") exceeded max execution time of ");
        sb.append(maxExecutionTime);
        sb.append(" ms");

        throw new ASAPExecTimeExceededException(sb.toString());
    }

    protected ASAP_PDU_1_0 readASAPPDU(long maxExecutionTime) throws ASAPException {
        // read asap pdu
        ASAPPDUReader pduReader = new ASAPPDUReader(this.protocol, this.is, this);

        this.runObservedThread(pduReader, maxExecutionTime);

        // exception caught while reading?
        if (pduReader.getIoException() != null || pduReader.getAsapException() != null) {
            Exception e = pduReader.getIoException() != null ?
                    pduReader.getIoException() : pduReader.getAsapException();

            throw new ASAPException("pdu reader caught exception: " + e.getLocalizedMessage());
        }

        ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();
        if(asappdu == null) {
            throw new ASAPException("could not read asap PDU");
        }

        return asappdu;
    }

    protected void runASAPExecutor(ASAP_PDU_1_0 asappdu, long maxExecutionTime) throws IOException, ASAPException {
        ASAPEngine engine = this.multiASAPEngineFS.getEngineByFormat(asappdu.getFormat());
        ASAPChunkReceivedListener listener = this.multiASAPEngineFS.getListenerByFormat(asappdu.getFormat());

        Thread executor = new ASAPPDUProcessor(asappdu, this.is, this.os,
                engine, new ASAP_Modem_Impl(),
                listener, // chunk received listener (can be null)
                this);

        this.runObservedThread(executor, maxExecutionTime);
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                          ASAP PDU Processing Thread                                   //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class ASAPPDUProcessor extends Thread {
        private final ASAP_PDU_1_0 asapPDU;
        private final InputStream is;
        private final OutputStream os;
        private final ASAPEngine engine;
        private final ASAP_1_0 protocol;
        private final ASAPChunkReceivedListener listener;
        private final ThreadFinishedListener threadFinishedListener;

        public ASAPPDUProcessor(ASAP_PDU_1_0 asapPDU, InputStream is, OutputStream os,
                                ASAPEngine engine, ASAP_1_0 protocol,
                                ASAPChunkReceivedListener listener,
                                ThreadFinishedListener threadFinishedListener) {
            this.asapPDU = asapPDU;
            this.is = is;
            this.os = os;
            this.engine = engine;
            this.protocol = protocol;
            this.listener = listener;
            this.threadFinishedListener = threadFinishedListener;

            if(listener != null) {
                System.out.println("ASAPPDUExecutor created: "
                        + "engine: " + engine.getClass().getSimpleName() + " | "
                        + "listener: " + listener.getClass().getSimpleName());
            } else {
                System.out.println("ASAPPDUExecutor created: "
                        + "engine: " + engine.getClass().getSimpleName() + " | "
                        + "no chunk received listener: ");
            }
        }

        private String getLogStart() {
            return this.getClass().getSimpleName() + ": ";
        }

        public void run() {
            try {
                String engineClass = engine.getClass().getSimpleName();
                switch (asapPDU.getCommand()) {
                    case ASAP_1_0.INTEREST_CMD:
                        System.out.println(this.getLogStart() + "call " + engineClass + ".handleASAPInterest()");
                        engine.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
                        System.out.println(this.getLogStart() + "done " + engineClass + ".handleASAPInterest()");
                        break;
                    case ASAP_1_0.OFFER_CMD:
                        System.out.println(this.getLogStart() + "call " + engineClass + ".handleASAPOffer()");
                        engine.handleASAPOffer((ASAP_OfferPDU_1_0) asapPDU, protocol, os);
                        System.out.println(this.getLogStart() + "done " + engineClass + ".handleASAPOffer()");
                        break;
                    case ASAP_1_0.ASSIMILATE_CMD:
                        System.out.println(this.getLogStart() + "call " + engineClass + ".handleASAPAssimilate()");
                        engine.handleASAPAssimilate((ASAP_AssimilationPDU_1_0) asapPDU, protocol, is, os,
                                listener);
                        System.out.println(this.getLogStart() + "done " + engineClass + ".handleASAPAssimilate()");
                        break;

                    default:
                        System.err.println(
                                this.getClass().getSimpleName() + ": " + "unknown ASAP command: " + asapPDU.getCommand());
                }
            }
            catch(IOException | ASAPException e) {
                System.err.println("Exception while processing ASAP PDU - close streams" + e.getLocalizedMessage());
            }

            if(this.threadFinishedListener != null) {
                this.threadFinishedListener.finished(this);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                          ASAP PDU Reader Thread                                       //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class ASAPPDUReader extends Thread {
        private final ASAP_1_0 protocol;
        private final InputStream is;
        private final ThreadFinishedListener pduReaderListener;
        private ASAP_PDU_1_0 asapPDU = null;
        private IOException ioException = null;
        private ASAPException asapException = null;

        ASAPPDUReader(ASAP_1_0 protocol, InputStream is, ThreadFinishedListener listener) {
            this.protocol = protocol;
            this.is = is;
            this.pduReaderListener = listener;
        }

        IOException getIoException() {
            return this.ioException;
        }

        ASAPException getAsapException() {
            return this.asapException;
        }

        ASAP_PDU_1_0 getASAPPDU() {
            return this.asapPDU;
        }

        public void run() {
            try {
                this.asapPDU = protocol.readPDU(is);
                this.pduReaderListener.finished(this);
            } catch (IOException e) {
                this.ioException = e;
            } catch (ASAPException e) {
                this.asapException = e;
            }
        }
    }
}
