package net.sharksystem.asap.protocol;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ASAPPersistentConnection implements ASAPConnection, Runnable, ThreadFinishedListener {
    private final InputStream is;
    private final OutputStream os;
    private final ASAPConnectionListener asapConnectionListener;
    private final MultiASAPEngineFS multiASAPEngineFS;
    private final ThreadFinishedListener threadFinishedListener;
    private final ASAP_1_0 protocol;
    private Thread managementThread = null;
    private final long maxExecutionTime;
    private String peer;

    private List<byte[]> onlineMessageList = new ArrayList<>();
    private List<ASAPOnlineMessageSource> onlineMessageSources = new ArrayList<>();
    private Thread threadWaiting4StreamsLock;

    public ASAPPersistentConnection(InputStream is, OutputStream os, MultiASAPEngineFS multiASAPEngineFS,
                                    ASAP_1_0 protocol,
                                    long maxExecutionTime, ASAPConnectionListener asapConnectionListener,
                                    ThreadFinishedListener threadFinishedListener) {
        this.is = is;
        this.os = os;
        this.multiASAPEngineFS = multiASAPEngineFS;
        this.protocol = protocol;
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
    public void removeOnlineMessageSource(ASAPOnlineMessageSource source) {
        this.onlineMessageSources.remove(source);
    }

    public boolean isSigned() {
        return false;
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

        // inform listener
        if(this.asapConnectionListener != null) {
            this.asapConnectionListener.asapConnectionTerminated(e, this);
        }

        if(this.threadFinishedListener != null) {
            this.threadFinishedListener.finished(Thread.currentThread());
        }
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
            } catch (IOException e) {
                this.caughtException = e;
            }
            finally {
                System.out.println(startLog() + "online sender releases lock");
                releaseStreamsLock();
                onlineMessageSenderThread = null;
                // are new message waiting in the meantime?
                checkRunningOnlineMessageSender();
            }
        }
    }

    OnlineMessageSenderThread onlineMessageSenderThread = null;
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

        try {
            // let engine write their interest
            this.multiASAPEngineFS.pushInterests(this.os);
        } catch (IOException | ASAPException e) {
            this.terminate("error when pushing interest: ", e);
            return;
        }

        /////////////////////////////// read
        while (true) {
            ASAPPDUReader pduReader = new ASAPPDUReader(protocol, is, this);
            try {
                this.runObservedThread(pduReader, this.maxExecutionTime);
            } catch (ASAPExecTimeExceededException e) {
                System.out.println(this.startLog() + "reading on stream took longer than allowed");
            }

            if (pduReader.getIoException() != null || pduReader.getAsapException() != null) {
                Exception e = pduReader.getIoException() != null ?
                        pduReader.getIoException() : pduReader.getAsapException();

                this.terminate("exception when reading from stream (stop asap session): ", e);
                return;
            }
            ASAP_PDU_1_0 asappdu = pduReader.getASAPPDU();
            /////////////////////////////// process
            if(asappdu != null) {
                this.setPeer(asappdu.getPeer());
                // process received pdu
                try {
                    Thread executor =
                            this.multiASAPEngineFS.getExecutorThread(asappdu, this.is, this.os, this);
                    // get exclusive access to streams
                    System.out.println(this.startLog() + "asap pdu executor going to wait for stream access");
                    this.wait4ExclusiveStreamsAccess();
                    try {
                        System.out.println(this.startLog() + "asap pdu executor got stream access");
                        this.runObservedThread(executor, maxExecutionTime);
                    } catch (ASAPExecTimeExceededException e) {
                        System.out.println(this.startLog() + "asap pdu processing took longer than allowed");
                        this.terminate("asap pdu processing took longer than allowed", e);
                        return;
                    } finally {
                        // wake waiting thread if any
                        this.releaseStreamsLock();
                        System.out.println(this.startLog() + "asap pdu executor release locks");
                    }
                }  catch (ASAPException e) {
                    this.terminate("serious problem when executing asap received pdu: ", e);
                    return;
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
        sb.append(this.peer);
        sb.append(" | ");

        return sb;
    }
}

