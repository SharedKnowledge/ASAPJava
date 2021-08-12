package net.sharksystem.asap.protocol;

import net.sharksystem.asap.EncounterConnectionType;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPInternalPeer;
import net.sharksystem.asap.engine.ASAPUndecryptableMessageHandler;
import net.sharksystem.asap.engine.EngineSetting;
import net.sharksystem.utils.Log;
import net.sharksystem.asap.crypto.ASAPKeyStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ASAPPersistentConnection extends ASAPProtocolEngine
        implements ASAPConnection, Runnable, ThreadFinishedListener {

    private final List<ASAPConnectionListener> asapConnectionListener;
    private final ASAPInternalPeer asapInternalPeer;
    private final ThreadFinishedListener threadFinishedListener;
    private final boolean encrypt;
    private final boolean sign;
    private final EncounterConnectionType connectionType;
    private Thread managementThread = null;
    private final long maxExecutionTime;
    private String encounteredPeer;

    private List<ASAPOnlineMessageSource> onlineMessageSources = new ArrayList<>();
    private Thread threadWaiting4StreamsLock;
    private boolean terminated = false;

    public ASAPPersistentConnection(InputStream is, OutputStream os, ASAPInternalPeer asapInternalPeer,
                                    ASAP_1_0 protocol, ASAPUndecryptableMessageHandler unencryptableMessageHandler,
                                    ASAPKeyStore ASAPKeyStore,
                                    long maxExecutionTime, ASAPConnectionListener asapConnectionListener,
                                    ThreadFinishedListener threadFinishedListener,
                                    boolean encrypt, boolean sign, EncounterConnectionType connectionType) {

        super(is, os, protocol, unencryptableMessageHandler, ASAPKeyStore);

        this.asapInternalPeer = asapInternalPeer;
        this.maxExecutionTime = maxExecutionTime;
        this.asapConnectionListener = new ArrayList<>();
        this.asapConnectionListener.add(asapConnectionListener);

        this.threadFinishedListener = threadFinishedListener;
        this.encrypt = encrypt;
        this.sign = sign;
        this.connectionType = connectionType;
    }

    public void addASAPConnectionListener(ASAPConnectionListener asapConnectionListener) {
        this.asapConnectionListener.add(asapConnectionListener);
    }

    public void removeASAPConnectionListener(ASAPConnectionListener asapConnectionListener) {
        this.asapConnectionListener.remove(asapConnectionListener);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + "(to: " + this.encounteredPeer + "): ";
    }

    private void setEncounteredPeer(String remotePeerName) {
        if(this.encounteredPeer == null) {

            this.encounteredPeer = remotePeerName;

            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("set remotePeerName after reading first asap message: ");
            sb.append(remotePeerName);
            Log.startLog(this, sb.toString());

            if(this.asapConnectionListener != null) {
                for(ASAPConnectionListener l : this.asapConnectionListener) {
                    l.asapConnectionStarted(remotePeerName, this);
                }
            }
        }
    }

    @Override
    public CharSequence getEncounteredPeer() {
        return this.encounteredPeer;
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
                for(ASAPConnectionListener l : this.asapConnectionListener) {
                    l.asapConnectionTerminated(e, this);
                }
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
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append(message);
        if(e != null) {
            sb.append(e.getLocalizedMessage());
        }

        sb.append(" | ");
        Log.writeLog(this, sb.toString());

        this.kill();
    }

    private void sendOnlineMessages() throws IOException {
        List<ASAPOnlineMessageSource> copy = onlineMessageSources;
        this.onlineMessageSources = new ArrayList<>();
        while(!copy.isEmpty()) {
            ASAPOnlineMessageSource asapOnline = copy.remove(0);
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("going to send online message");
            Log.writeLog(this, sb.toString());
            asapOnline.sendStoredMessages(this, this.os);
        }
    }

    private class OnlineMessageSenderThread extends Thread {
        public Exception caughtException = null;
        public void run() {
            try {
                // get exclusive access to streams
                Log.writeLog(this, getLogStart() + "online sender is going to wait for stream access");
                wait4ExclusiveStreamsAccess();
                Log.writeLog(this, getLogStart() + "online sender got stream access");
                sendOnlineMessages();
                // prepare a graceful death
                onlineMessageSenderThread = null;
                // are new message waiting in the meantime?
                checkRunningOnlineMessageSender();
            } catch (IOException e) {
                terminate("could not write data into stream", e);
            }
            finally {
                Log.writeLog(this, getLogStart() + "online sender releases lock");
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
        ASAP_1_0 protocol = new ASAP_Modem_Impl(this.ASAPKeyStore, this.undecryptableMessageHandler);

        try {
            // let engine write their interest - at least management interest is sent which als introduces
            // this peer to the other one
            this.asapInternalPeer.pushInterests(this.os);
        } catch (IOException | ASAPException e) {
            this.terminate("error when pushing interest: ", e);
            //e.printStackTrace();
            return;
        }

        /////////////////////////////// read
        while (!this.terminated) {
            this.pduReader = new ASAPPDUReader(protocol, is, this);
            try {
                Log.writeLog(this, this.getLogStart() + "start reading");
                this.runObservedThread(pduReader, this.maxExecutionTime);
            } catch (ASAPExecTimeExceededException e) {
                Log.writeLog(this, this.getLogStart() + "reading on stream took longer than allowed");
            }

            Log.writeLog(this, this.getLogStart() + "back from reading");
            if(terminated) break; // thread could be killed in the meantime

            if (pduReader.getIoException() != null || pduReader.getAsapException() != null) {
                Exception e = pduReader.getIoException() != null ?
                        pduReader.getIoException() : pduReader.getAsapException();

                try {
                    this.is.close();
                } catch (IOException exception) {
                    Log.writeLog(this, this.getLogStart()
                            + "tried to close stream after exception caught: " + exception.getLocalizedMessage());
                }

                this.terminate("exception when reading from stream (stop asap session, stream closed): ", e);
                break;
            }

            ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();
            /////////////////////////////// process
            if(asappdu != null) {
                Log.writeLog(this, this.getLogStart() + "read valid pdu");
                this.setEncounteredPeer(asappdu.getSender());

                try {
                    this.executor = new ASAPPDUExecutor(asappdu,
                                        this.encounteredPeer,
                                        this.is, this.os,
                                        this.asapInternalPeer.getEngineSettings(asappdu.getFormat()),
                                        protocol,this.connectionType, this);

                    // get exclusive access to streams
                    Log.writeLog(this, this.getLogStart() + "asap pdu executor going to wait for stream access");
                    this.wait4ExclusiveStreamsAccess();
                    try {
                        Log.writeLog(this, this.getLogStart() + "asap pdu executor got stream access - process pdu");
                        this.runObservedThread(executor, maxExecutionTime);
                    } catch (ASAPExecTimeExceededException e) {
                        Log.writeLog(this, this.getLogStart() + "asap pdu processing took longer than allowed");
                        this.terminate("asap pdu processing took longer than allowed", e);
                        break;
                    } finally {
                        // wake waiting thread if any
                        this.releaseStreamsLock();
                        Log.writeLog(this, this.getLogStart() + "asap pdu executor release locks");
                    }
                } catch (ASAPException e) {
                    Log.writeLog(this, this.getLogStart() + " problem when executing asap received pdu: " + e);
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

    // why not simply synchronized(this) { ... }?? OK, that code works but look complicated (thsc42)
    private void wait4ExclusiveStreamsAccess() {
        // synchronize with other thread using streams
        Thread threadUsingStreams = this.getThreadUsingStreams(Thread.currentThread());

        // got lock - go ahead
        if(threadUsingStreams == null) {
            return;
        }

        // there is another thread - wait until it dies
        do {
            Log.writeLog(this, this.getLogStart() + "enter waiting loop for exclusive stream access");
            // wait
            try {
                this.threadWaiting4StreamsLock = Thread.currentThread();
                threadUsingStreams.join();
            } catch (InterruptedException e) {
                Log.writeLog(this, this.getLogStart() + "woke up from join");
            }
            finally {
                this.threadWaiting4StreamsLock = null;
            }
            // try again
            Log.writeLog(this, this.getLogStart() + "try to get streams access again");
            threadUsingStreams = this.getThreadUsingStreams(Thread.currentThread());
        } while(threadUsingStreams != null);
        Log.writeLog(this, this.getLogStart() + "leave waiting loop for exclusive stream access");
    }

    private void releaseStreamsLock() {
        this.threadUsingStreams = null; // take me out
        if(this.threadWaiting4StreamsLock != null) {
            Log.writeLog(this, this.getLogStart() + "wake waiting thread");
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

    private class ASAPPDUExecutor extends Thread {
        private final ASAP_PDU_1_0 asapPDU;
        private final InputStream is;
        private final OutputStream os;
        private final EngineSetting engineSetting;
        private final ASAP_1_0 protocol;
        private final ThreadFinishedListener threadFinishedListener;
        private final String encounteredPeer;
        private final EncounterConnectionType connectionType;

        public ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, String encounteredPeer, InputStream is, OutputStream os,
                EngineSetting engineSetting, ASAP_1_0 protocol,
                EncounterConnectionType connectionType, ThreadFinishedListener threadFinishedListener) {
            this.asapPDU = asapPDU;
            this.encounteredPeer = encounteredPeer;
            this.is = is;
            this.os = os;
            this.engineSetting = engineSetting;
            this.protocol = protocol;
            this.connectionType = connectionType;
            this.threadFinishedListener = threadFinishedListener;

            StringBuilder sb = new StringBuilder();
            sb.append(getLogStart());
            sb.append("ASAPPDUExecutor: ");
            sb.append("engine: " + engineSetting.engine.getClass().getSimpleName() + " | ");
            if(engineSetting.listener != null) {
                sb.append("listener: " + engineSetting.listener.getClass().getSimpleName() + " | ");
            }
            sb.append("folder: " + engineSetting.folder);

            Log.writeLog(this, sb.toString());
        }

        private void finish() {
            if(this.threadFinishedListener != null) {
                this.threadFinishedListener.finished(this);
            }
        }

        public void run() {
            if(engineSetting.engine == null) {
                Log.writeLogErr(this,  "ASAPPDUExecutor called without engine set - fatal");
                this.finish();
                return;
            }

            Log.writeLog(this, getLogStart() + "ASAPPDUExecutor calls engine: "
                    + engineSetting.engine.getClass().getSimpleName());

            try {
                switch (asapPDU.getCommand()) {
                    // TODO add encrypt / sign as parameter..
                    case ASAP_1_0.INTEREST_CMD:
                        Log.writeLog(this, getLogStart() + "ASAPPDUExecutor call handleASAPInterest");
                        engineSetting.engine.handleASAPInterest(
                                (ASAP_Interest_PDU_1_0) asapPDU, this.protocol,
                                this.encounteredPeer,
                                this.os,
                                this.connectionType);
                        break;
                    case ASAP_1_0.ASSIMILATE_CMD:
                        Log.writeLog(this, getLogStart() + "ASAPPDUExecutor call handleASAPAssimilate");
                        engineSetting.engine.handleASAPAssimilate(
                                (ASAP_AssimilationPDU_1_0) this.asapPDU,
                                this.protocol,
                                this.encounteredPeer, this.is, this.os,
                                this.connectionType,
                                this.engineSetting.listener);
                        break;

                    default:
                        Log.writeLogErr(this, "unknown ASAP command: " + asapPDU.getCommand());
                }
            }
            catch(ASAPException asape) {
                Log.writeLogErr(this, "asap exception while processing PDU - but go ahead: "
                        + asape.getLocalizedMessage());
            }
            catch(IOException ioe) {
                Log.writeLogErr(this,"IOException while processing ASAP PDU - close streams: "
                        + ioe.getLocalizedMessage());
                try {
                    os.close(); // more important to close than input stream - do it first
                    is.close();
                } catch (IOException ex) {
                    Log.writeLog(this, ex.getLocalizedMessage());
                    //ex.printStackTrace();
                }
            }
            finally {
                this.finish();
            }
        }
    }

    /**
     * Waits for one PDU and goes away if read.
     */
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
                //boolean dropped = false;
                //do {
                    //if(dropped) Log.writeLog(this, "dropped pdu: no sufficient encryption");
                    this.asapPDU = protocol.readPDU(is);
                    //dropped = true;
                //} // refuse lesser security settings and read next pdu
                //while((encrypt && !this.asapPDU.encrypted()) || (sign && !this.asapPDU.verified()));
            } catch (IOException e) {
                this.ioException = e;
                Log.writeLog(this, ASAPPersistentConnection.this.getLogStart()
                        + "IOException when reading from stream");
            } catch (ASAPException e) {
                Log.writeLog(this, ASAPPersistentConnection.this.getLogStart()
                        + "ASAPException when reading from stream");
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
