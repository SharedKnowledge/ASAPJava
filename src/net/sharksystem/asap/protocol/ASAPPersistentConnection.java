package net.sharksystem.asap.protocol;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ASAPPersistentConnection extends ASAPProtocolEngine
        implements ASAPConnection, Runnable, ThreadFinishedListener {

    private final ASAPConnectionListener asapConnectionListener;
    private final MultiASAPEngineFS multiASAPEngineFS;
    private final ThreadFinishedListener threadFinishedListener;
    private Thread managementThread = null;
    private final long maxExecutionTime;
    private String remotePeer;

    private List<ASAPOnlineMessageSource> onlineMessageSources = new ArrayList<>();
    private Thread threadWaiting4StreamsLock;
    private boolean terminated = false;

    public ASAPPersistentConnection(InputStream is, OutputStream os, MultiASAPEngineFS multiASAPEngineFS,
                                    ASAP_1_0 protocol,
                                    long maxExecutionTime, ASAPConnectionListener asapConnectionListener,
                                    ThreadFinishedListener threadFinishedListener) {

        super(is, os, protocol);

        this.multiASAPEngineFS = multiASAPEngineFS;
        this.maxExecutionTime = maxExecutionTime;
        this.asapConnectionListener = asapConnectionListener;
        this.threadFinishedListener = threadFinishedListener;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + "(connected to: " + this.remotePeer + "): ";
    }

    private void setRemotePeer(String remotePeerName) {
        if(this.remotePeer == null) {

            this.remotePeer = remotePeerName;

            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("set remotePeerName after reading first asap message: ");
            sb.append(remotePeerName);
            System.out.println(sb.toString());

            if(this.asapConnectionListener != null) {
                this.asapConnectionListener.asapConnectionStarted(remotePeerName, this);
            }
        }
    }

    @Override
    public CharSequence getRemotePeer() {
        return this.remotePeer;
    }

    @Override
    public void removeOnlineMessageSource(ASAPOnlineMessageSource source) {
        this.onlineMessageSources.remove(source);
    }

    public boolean isSigned() {
        return false;
    }

    @Override
    public void kill() {
        this.kill(new ASAPException("kill called from outside asap connection"));
    }

    public void kill(Exception e) {
        if(!this.terminated) {
            this.terminated = true;
            // kill reader - proofed to be useful in a bluetooth environment
            if(this.pduReader != null && this.pduReader.isAlive()) {
                this.pduReader.interrupt();
            }
            if(this.managementThread != null && this.managementThread.isAlive()) {
                this.managementThread.interrupt();
            }
            // inform listener
            if (this.asapConnectionListener != null) {
                this.asapConnectionListener.asapConnectionTerminated(e, this);
            }

            if (this.threadFinishedListener != null) {
                this.threadFinishedListener.finished(Thread.currentThread());
            }
        }
    }

    @Override
    public void finished(Thread t) {
        if(this.managementThread != null) {
            this.managementThread.interrupt();
        }
    }

    private void terminate(String message, Exception e) {
        // write log
        StringBuilder sb = this.startLog();
        sb.append(message);
        if(e != null) {
            sb.append(e.getLocalizedMessage());
        }

        sb.append(" | ");
        System.out.println(sb.toString());

        this.kill();
    }

    private void sendOnlineMessages() throws IOException {
        List<ASAPOnlineMessageSource> copy = onlineMessageSources;
        this.onlineMessageSources = new ArrayList<>();
        while(!copy.isEmpty()) {
            ASAPOnlineMessageSource asapOnline = copy.remove(0);
            StringBuilder sb = this.startLog();
            sb.append("going to send online message");
            System.out.println(sb.toString());
            asapOnline.sendMessages(this, this.os);
        }
    }

    private class OnlineMessageSenderThread extends Thread {
        public Exception caughtException = null;
        public void run() {
            try {
                // get exclusive access to streams
                System.out.println(startLog() + "online sender is going to wait for stream access");
                wait4ExclusiveStreamsAccess();
                System.out.println(startLog() + "online sender got stream access");
                sendOnlineMessages();
                // prepare a graceful death
                onlineMessageSenderThread = null;
                // are new message waiting in the meantime?
                checkRunningOnlineMessageSender();
            } catch (IOException e) {
                terminate("could not write data into stream", e);
            }
            finally {
                System.out.println(startLog() + "online sender releases lock");
                releaseStreamsLock();
            }
        }
    }

    private OnlineMessageSenderThread onlineMessageSenderThread = null;
    private ASAPPDUReader pduReader = null;
    Thread executor = null;

    @Override
    public void addOnlineMessageSource(ASAPOnlineMessageSource source) {
        this.onlineMessageSources.add(source);
        this.checkRunningOnlineMessageSender();
    }

    private synchronized void checkRunningOnlineMessageSender() {
        if(this.onlineMessageSenderThread == null
                && this.onlineMessageSources != null && this.onlineMessageSources.size() > 0) {
            this.onlineMessageSenderThread = new OnlineMessageSenderThread();
            this.onlineMessageSenderThread.start();
        }
    }

    public void run() {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        // introduce yourself
        CharSequence owner = this.multiASAPEngineFS.getOwner();
        if(owner != null && owner.length() > 0) {
            try {
                System.out.println(this.getLogStart() + "send introduction ASAP Offer; owner: " + owner);
                this.sendIntroductionOffer(owner, false);
            } catch (IOException e) {
                this.terminate("io error when sending introduction offering: ", e);
                return;
            } catch (ASAPException e) {
                System.out.println(this.getLogStart()
                        + "could not send introduction offer: " + e.getLocalizedMessage());
                // go ahead - no io problem
            }
        }

        try {
            // let engine write their interest
            this.multiASAPEngineFS.pushInterests(this.os);
        } catch (IOException | ASAPException e) {
            this.terminate("error when pushing interest: ", e);
            return;
        }

        /////////////////////////////// read
        while (!this.terminated) {
            this.pduReader = new ASAPPDUReader(protocol, is, this);
            try {
                System.out.println(this.startLog() + "start reading");
                this.runObservedThread(pduReader, this.maxExecutionTime);
            } catch (ASAPExecTimeExceededException e) {
                System.out.println(this.startLog() + "reading on stream took longer than allowed");
            }

            System.out.println(this.getLogStart() + "back from reading");
            if(terminated) break; // could be killed in the meantime

            if (pduReader.getIoException() != null || pduReader.getAsapException() != null) {
                Exception e = pduReader.getIoException() != null ?
                        pduReader.getIoException() : pduReader.getAsapException();

                this.terminate("exception when reading from stream (stop asap session): ", e);
                break;
            }

            ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();
            /////////////////////////////// process
            if(asappdu != null) {
                System.out.println(this.getLogStart() + "read valid pdu");
                this.setRemotePeer(asappdu.getPeer());
                // process received pdu

                if(asappdu.getFormat().equalsIgnoreCase(ASAP_1_0.ASAP_MANAGEMENT_FORMAT.toString())) {
                    System.out.println(this.getLogStart()
                            + "got asap management message - not processed, took remote peer name only");
                } else {
                    try {
                        this.executor = new ASAPPDUExecutor(asappdu,
                                            this.is, this.os,
                                            this.multiASAPEngineFS.getEngineSettings(asappdu.getFormat()),
                                            protocol,this);

                        // get exclusive access to streams
                        System.out.println(this.startLog() + "asap pdu executor going to wait for stream access");
                        this.wait4ExclusiveStreamsAccess();
                        try {
                            System.out.println(this.startLog() + "asap pdu executor got stream access - process pdu");
                            this.runObservedThread(executor, maxExecutionTime);
                        } catch (ASAPExecTimeExceededException e) {
                            System.out.println(this.startLog() + "asap pdu processing took longer than allowed");
                            this.terminate("asap pdu processing took longer than allowed", e);
                            break;
                        } finally {
                            // wake waiting thread if any
                            this.releaseStreamsLock();
                            System.out.println(this.startLog() + "asap pdu executor release locks");
                        }
                    } catch (ASAPException e) {
                        System.out.println(this.getLogStart() + " problem when executing asap received pdu: " + e);
                    }
                }
            }
        }
    }

    private Thread threadUsingStreams = null;
    private synchronized Thread getThreadUsingStreams(Thread t) {
        if(this.threadUsingStreams == null) {
            this.threadUsingStreams = t;
            return null;
        }

        return this.threadUsingStreams;
    }

    private void wait4ExclusiveStreamsAccess() {
        // synchronize with other thread using streams
        Thread threadUsingStreams = this.getThreadUsingStreams(Thread.currentThread());

        // got lock - go ahead
        if(threadUsingStreams == null) {
            return;
        }

        // there is another stream - wait until it dies
        do {
            System.out.println(this.getLogStart() + "enter waiting loop for exclusive stream access");
            // wait
            try {
                this.threadWaiting4StreamsLock = Thread.currentThread();
                threadUsingStreams.join();
            } catch (InterruptedException e) {
                System.out.println(this.getLogStart() + "woke up from join");
            }
            finally {
                this.threadWaiting4StreamsLock = null;
            }
            // try again
            System.out.println(this.getLogStart() + "try to get streams access again");
            threadUsingStreams = this.getThreadUsingStreams(Thread.currentThread());
        } while(threadUsingStreams != null);
        System.out.println(this.getLogStart() + "leave waiting loop for exclusive stream access");
    }

    private void releaseStreamsLock() {
        this.threadUsingStreams = null; // take me out
        if(this.threadWaiting4StreamsLock != null) {
            System.out.println(this.getLogStart() + "wake waiting thread");
            this.threadWaiting4StreamsLock.interrupt();
        }
    }

    private void runObservedThread(Thread t, long maxExecutionTime) throws ASAPExecTimeExceededException {
        this.managementThread = Thread.currentThread();
        t.start();

        // wait for reader
        try {
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

    private StringBuilder startLog() {
        StringBuilder sb = net.sharksystem.asap.util.Log.startLog(this);
        sb.append(" recipient: ");
        sb.append(this.remotePeer);
        sb.append(" | ");

        return sb;
    }

    private class ASAPPDUExecutor extends Thread {
        private final ASAP_PDU_1_0 asapPDU;
        private final InputStream is;
        private final OutputStream os;
        private final EngineSetting engineSetting;
        private final ASAP_1_0 protocol;
        private final ThreadFinishedListener threadFinishedListener;

        public ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, InputStream is, OutputStream os,
                               EngineSetting engineSetting, ASAP_1_0 protocol,
                               ThreadFinishedListener threadFinishedListener) {
            this.asapPDU = asapPDU;
            this.is = is;
            this.os = os;
            this.engineSetting = engineSetting;
            this.protocol = protocol;
            this.threadFinishedListener = threadFinishedListener;

            StringBuilder sb = new StringBuilder();
            sb.append("ASAPPDUExecutor created - ");
            sb.append("folder: " + engineSetting.folder + " | ");
            sb.append("engine: " + engineSetting.engine.getClass().getSimpleName() + " | ");
            if(engineSetting.listener != null) {
                sb.append("listener: " + engineSetting.listener.getClass().getSimpleName());
            }

            System.out.println(sb.toString());
        }

        private void finish() {
            if(this.threadFinishedListener != null) {
                this.threadFinishedListener.finished(this);
            }
        }

        public void run() {
            if(engineSetting.engine == null) {
                System.err.println("ASAPPDUExecutor called without engine set - fatal");
                this.finish();
                return;
            }

            System.out.println("ASAPPDUExecutor calls engine: " + engineSetting.engine.getClass().getSimpleName());

            try {
                switch (asapPDU.getCommand()) {
                    case ASAP_1_0.INTEREST_CMD:
                        System.out.println("ASAPPDUExecutor call handleASAPInterest");
                        engineSetting.engine.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
                        break;
                    case ASAP_1_0.OFFER_CMD:
                        System.out.println("ASAPPDUExecutor call handleASAPOffer");
                        engineSetting.engine.handleASAPOffer((ASAP_OfferPDU_1_0) asapPDU, protocol, os);
                        break;
                    case ASAP_1_0.ASSIMILATE_CMD:
                        System.out.println("ASAPPDUExecutor call handleASAPAssimilate");
                        engineSetting.engine.handleASAPAssimilate((ASAP_AssimilationPDU_1_0) asapPDU, protocol, is, os,
                                engineSetting.listener);
                        break;

                    default:
                        System.err.println(
                                this.getClass().getSimpleName() + ": " + "unknown ASAP command: " + asapPDU.getCommand());
                }
            }
            catch(IOException | ASAPException e) {
                System.err.println("Exception while processing ASAP PDU - close streams" + e.getLocalizedMessage());
                try {
                    os.close(); // more important to close than input stream - do it first
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            finally {
                this.finish();
            }
        }
    }

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
    //            this.pduReaderListener.finished(this);
            } catch (IOException e) {
                this.ioException = e;
            } catch (ASAPException e) {
                this.asapException = e;
            }
            finally {
                if(this.pduReaderListener != null) {
                    this.pduReaderListener.finished(this);
                }
            }
        }
    }
}
