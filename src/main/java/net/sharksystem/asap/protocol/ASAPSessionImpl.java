package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPInternalPeer;
import net.sharksystem.asap.engine.ASAPUndecryptableMessageHandler;
import net.sharksystem.asap.engine.EngineSetting;
import net.sharksystem.utils.Log;
import net.sharksystem.asap.crypto.ASAPKeyStore;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ASAPSessionImpl extends ASAPProtocolEngine
        implements ASAPConnection, Runnable, ThreadFinishedListener {

    private final List<ASAPConnectionListener> asapConnectionListener;
    private final ASAPInternalPeer asapInternalPeer;
    private final ThreadFinishedListener threadFinishedListener;
    private final boolean encrypt;
    private final boolean sign;
    private final ASAPEncounterConnectionType connectionType;
    private Thread managementThread = null;
    private final long maxExecutionTime;
    private String encounteredPeer;

    private List<ASAPOnlineMessageSource> onlineMessageSources = new ArrayList<>();
    private Thread threadWaiting4StreamsLock;
    private boolean terminated = false;

    public ASAPSessionImpl(InputStream is, OutputStream os, ASAPInternalPeer asapInternalPeer,
                           ASAP_1_0 protocol, ASAPUndecryptableMessageHandler unencryptableMessageHandler,
                           ASAPKeyStore ASAPKeyStore,
                           long maxExecutionTime, ASAPConnectionListener asapConnectionListener,
                           ThreadFinishedListener threadFinishedListener,
                           boolean encrypt, boolean sign, ASAPEncounterConnectionType connectionType) {

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

    private String getLogParameter() {
        String s = "to: ";
        s += this.encounteredPeer != null ? this.encounteredPeer : "unknown yet";
        return s;
    }

    private void setEncounteredPeer(String remotePeerName) {
        if(this.encounteredPeer == null) {

            this.encounteredPeer = remotePeerName;

            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogParameter());
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
    public ASAPEncounterConnectionType getASAPEncounterConnectionType() {
        return this.connectionType;
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

    private void terminate(String message, Throwable t) {
        // write log
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        if(t != null) {
            sb.append(" | issued by: ");
            sb.append(t.getClass().getSimpleName());
            sb.append(": ");
            sb.append(t.getLocalizedMessage());
            // debugging
            //t.printStackTrace();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            sb.append("\n>>>>>>>>>>>>> stack trace:\n");
            sb.append(baos.toString());
            sb.append("<<<<<<<<<<<< stack trace");
        }
        Log.writeLog(this, this.getLogParameter(), sb.toString());

        this.kill();
    }

    private void sendOnlineMessages() throws IOException {
        List<ASAPOnlineMessageSource> copy = this.onlineMessageSources;
        this.onlineMessageSources = new ArrayList<>();
        while(!copy.isEmpty()) {
            ASAPOnlineMessageSource asapOnline = copy.remove(0);
            StringBuilder sb = new StringBuilder();
            sb.append("going to send online message");
            Log.writeLog(this, this.getLogParameter(), sb.toString());
            asapOnline.sendStoredMessages(this, this.os);
        }
    }

    private class OnlineMessageSenderThread extends Thread {
        public void run() {
            try {
                // get exclusive access to streams
                Log.writeLog(this, getLogParameter(), "online sender is going to wait for stream access");
                wait4ExclusiveStreamsAccess();
                Log.writeLog(this, getLogParameter(), "online sender got stream access");
                sendOnlineMessages();
                // prepare a graceful death
                onlineMessageSenderThread = null;
                // are new message waiting in the meantime?
                checkRunningOnlineMessageSender();
            } catch (IOException e) {
                terminate("could not write data into stream", e);
            }
            finally {
                Log.writeLog(this, getLogParameter(), "online sender releases lock");
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
            Throwable unexpectedThrowable = null;
            try {
                Log.writeLog(this, this.getLogParameter(), "start reading");
                this.runObservedThread(pduReader, this.maxExecutionTime);
            } catch (ASAPExecTimeExceededException e) {
                Log.writeLog(this, this.getLogParameter(),  "reading on stream took longer than allowed");
            }
            catch(Throwable t) {
                unexpectedThrowable = t;
                Log.writeLog(this, this.getLogParameter(),  "while reading PDU: "
                        + t.getClass().getSimpleName() + ": " + t.getLocalizedMessage());
            }

            Log.writeLog(this, this.getLogParameter(),  "back from reading");
            if(terminated) break; // thread could be killed in the meantime

            if (unexpectedThrowable != null || pduReader.getIoException() != null || pduReader.getAsapException() != null) {
                Log.writeLog(this, this.getLogParameter(), "connection broken");

                Throwable problem = pduReader.getIoException() != null ?
                        pduReader.getIoException() : pduReader.getAsapException();

                if(problem == null) problem = unexpectedThrowable;

                try {
                    Log.writeLog(this, this.getLogParameter(), "close input stream");
                    this.is.close();
                } catch (IOException exception) {
                    Log.writeLog(this, this.getLogParameter(),
                            "tried to close stream after exception caught: " + exception.getLocalizedMessage());
                }

                this.terminate("problem when reading from stream (close asap session and stream): ", problem);
                break;
            }
            
            ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();
            /////////////////////////////// process
            if(asappdu != null) {
                Log.writeLog(this, this.getLogParameter(),
                        "read valid pdu, remember meeting this peer and going to process pdu");
                this.setEncounteredPeer(asappdu.getSender());

                EngineSetting engineSettings = null;
                try {
                    engineSettings = this.asapInternalPeer.getEngineSettings(asappdu.getFormat());
                } catch(ASAPException e) {
                    // can happen with transient messages
                    Log.writeLog(this, this.getLogParameter(),  "no engine setting - set defaults");
                }

                try {
                    if(engineSettings == null) {
                        ASAPEngine asapEngine = this.asapInternalPeer.getASAPEngine(asappdu.getFormat());
                        engineSettings = this.asapInternalPeer.getEngineSettings(asappdu.getFormat());
                        engineSettings.engine = asapEngine;
                        Log.writeLog(this, this.getLogParameter(), engineSettings.toString());
                    }

                    this.executor = new ASAPPDUExecutor(asappdu,
                        this.encounteredPeer, this.is, this.os, engineSettings,
                        protocol,this.connectionType, this);

                    // get exclusive access to streams
                    Log.writeLog(this, this.getLogParameter(),  "asap pdu executor going to wait for stream access");
                    this.wait4ExclusiveStreamsAccess();
                    try {
                        Log.writeLog(this, this.getLogParameter(),  "asap pdu executor got stream access - process pdu");
                        this.runObservedThread(executor, maxExecutionTime);
                    } catch (ASAPExecTimeExceededException e) {
                        Log.writeLog(this, this.getLogParameter(),  "asap pdu processing took longer than allowed");
                        this.terminate("asap pdu processing took longer than allowed", e);
                        break;
                    } finally {
                        // wake waiting thread if any
                        this.releaseStreamsLock();
                        Log.writeLog(this, this.getLogParameter(),  "asap pdu executor release locks");
                    }
                } catch (ASAPException | IOException e) {
                    // Log.writeLog(this, this.getLogParameter(),  "problem when executing asap received pdu: " + e);
                    this.terminate("problem when executing asap received pdu: ", e);
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

    // why not simply synchronized(this) { ... }?? OK, that code works but looks complicated (thsc42)
    private void wait4ExclusiveStreamsAccess() {
        // synchronize with other thread using streams
        Thread threadUsingStreams = this.getThreadUsingStreams(Thread.currentThread());

        // got lock - go ahead
        if(threadUsingStreams == null) {
            return;
        }

        // there is another thread - wait until it dies
        do {
            Log.writeLog(this, this.getLogParameter(),  "enter waiting loop for exclusive stream access");
            // wait
            try {
                this.threadWaiting4StreamsLock = Thread.currentThread();
                threadUsingStreams.join();
            } catch (InterruptedException e) {
                Log.writeLog(this, this.getLogParameter(),  "woke up from join");
            }
            finally {
                this.threadWaiting4StreamsLock = null;
            }
            // try again
            Log.writeLog(this, this.getLogParameter(),  "try to get streams access again");
            threadUsingStreams = this.getThreadUsingStreams(Thread.currentThread());
        } while(threadUsingStreams != null);
        Log.writeLog(this, this.getLogParameter(),  "leave waiting loop for exclusive stream access");
    }

    private void releaseStreamsLock() {
        this.threadUsingStreams = null; // take me out
        if(this.threadWaiting4StreamsLock != null) {
            Log.writeLog(this, this.getLogParameter(),  "wake waiting thread");
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
        private final ASAPEncounterConnectionType connectionType;

        public ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, String encounteredPeer, InputStream is, OutputStream os,
                               EngineSetting engineSetting, ASAP_1_0 protocol,
                               ASAPEncounterConnectionType connectionType, ThreadFinishedListener threadFinishedListener) {
            this.asapPDU = asapPDU;
            this.encounteredPeer = encounteredPeer;
            this.is = is;
            this.os = os;
            this.engineSetting = engineSetting;
            this.protocol = protocol;
            this.connectionType = connectionType;
            this.threadFinishedListener = threadFinishedListener;

            StringBuilder sb = new StringBuilder();
            sb.append(getLogParameter());
            sb.append("ASAPPDUExecutor: ");
            sb.append("engine: " + engineSetting.engine.getClass().getSimpleName() + " | ");
            if(engineSetting.listener != null) {
                sb.append("listener: " + engineSetting.listener.getClass().getSimpleName() + " | ");
            }
            sb.append("folder: " + engineSetting.folder);

            Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(), sb.toString());
        }

        private void finish() {
            if(this.threadFinishedListener != null) {
                this.threadFinishedListener.finished(this);
            }
        }

        public void run() {
            if(engineSetting.engine == null) {
                Log.writeLogErr(this, ASAPSessionImpl.this.getLogParameter(),
                        "ASAPPDUExecutor called without engine set - fatal");
                this.finish();
                return;
            }

            Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(),
                    "ASAPPDUExecutor calls engine: " + engineSetting.engine.getClass().getSimpleName());

            try {
                switch (asapPDU.getCommand()) {
                    // TODO add encrypt / sign as parameter..
                    case ASAP_1_0.INTEREST_CMD:
                        Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(),
                                "ASAPPDUExecutor call handleASAPInterest");
                        engineSetting.engine.handleASAPInterest(
                                (ASAP_Interest_PDU_1_0) asapPDU, this.protocol,
                                this.encounteredPeer,
                                this.os,
                                this.connectionType);
                        break;
                    case ASAP_1_0.ASSIMILATE_CMD:
                        Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(),
                                "ASAPPDUExecutor call handleASAPAssimilate");
                        engineSetting.engine.handleASAPAssimilate(
                                (ASAP_AssimilationPDU_1_0) this.asapPDU,
                                this.protocol,
                                this.encounteredPeer, this.is, this.os,
                                this.connectionType,
                                this.engineSetting.listener);
                        break;

                    default:
                        Log.writeLogErr(this, ASAPSessionImpl.this.getLogParameter(),
                                "unknown ASAP command: " + asapPDU.getCommand());
                }
            }
            catch(ASAPException asape) {
                Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(),
                        "while processing PDU (go ahead): " + asape.getLocalizedMessage());
            }
            catch(IOException ioe) {
                Log.writeLogErr(this, ASAPSessionImpl.this.getLogParameter(),
                        "IOException while processing ASAP PDU - close streams: " + ioe.getLocalizedMessage());
                try {
                    os.close(); // more important to close than input stream - try first
                    is.close();
                } catch (IOException ex) {
                    Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(), ex.getLocalizedMessage());
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
                Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(),
                        "IOException when reading from stream");
            } catch (ASAPException e) {
                Log.writeLog(this, ASAPSessionImpl.this.getLogParameter(),
                        "ASAPException when reading from stream");
                this.asapException = e;
            }
            finally {
                if(this.pduReaderListener != null) {
                    this.pduReaderListener.finished(this);
                }
            }
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.asapInternalPeer.getOwner());
        sb.append(" <--> ");
        if(this.encounteredPeer == null) sb.append("<unkown yet>");
        else sb.append(this.encounteredPeer);
        sb.append(" | type: ");
        sb.append(this.connectionType);
        sb.append(" | encrypted: ");
        sb.append(this.encrypt);
        sb.append(" | sign: ");
        sb.append(this.sign);
        return sb.toString();
    }
}
