package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiASAPEngineFS_Impl implements MultiASAPEngineFS,
        ASAPOnlineConnectionListener, ASAPStartupConnectionListener,
        ThreadFinishedListener {
    private final CharSequence rootFolderName;
    private final ASAPChunkReceivedListener listener;
    private CharSequence owner;
    private HashMap<CharSequence, EngineSetting> folderMap;
    private final long maxExecutionTime;

    public static MultiASAPEngineFS createMultiEngine(CharSequence owner, CharSequence rootFolder, long maxExecutionTime,
                                                      ASAPChunkReceivedListener listener) throws ASAPException, IOException {
        return new MultiASAPEngineFS_Impl(owner, rootFolder, maxExecutionTime, listener);
    }

    public static MultiASAPEngineFS createMultiEngine(CharSequence folder, ASAPChunkReceivedListener listener)
            throws ASAPException, IOException {

        return MultiASAPEngineFS_Impl.createMultiEngine(ASAPEngine.DEFAULT_OWNER, folder,
                DEFAULT_MAX_PROCESSING_TIME, listener);
    }

    /**
     * assumed that a number of asap storages are already exists in subdirectories of the
     * root directory. setting list can be created by iterating those storages.
     * @param rootFolderName
     */
    MultiASAPEngineFS_Impl(CharSequence owner, CharSequence rootFolderName, long maxExecutionTime,
                                  ASAPChunkReceivedListener listener) throws ASAPException, IOException {

//        this.owner = ASAPEngine.DEFAULT_OWNER; // probably dummy name
        this.owner = owner; // probably dummy name
        this.maxExecutionTime = maxExecutionTime;
        this.rootFolderName = rootFolderName;
        this.listener = listener;

        File rootFolder = new File(rootFolderName.toString());

        if(!rootFolder.exists()) {
            // create
            rootFolder.mkdirs();
        }

        if (!rootFolder.isDirectory()) {
            throw new ASAPException("exists but is not a directory: " + rootFolderName);
        }

        this.setupFolderMap();
    }

    private void setupFolderMap() throws IOException, ASAPException {
        this.folderMap = new HashMap<>();
        File rootFolder = new File(rootFolderName.toString());

        System.out.println(this.getLogStart() + "iterate subfolder in " + this.rootFolderName);
        File[] files = rootFolder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String fileName = file.getCanonicalPath();
                System.out.println(this.getLogStart() + "setup engine for " + fileName);
                ASAPEngine engine = ASAPEngineFS.getExistingASAPEngineFS(fileName);
                EngineSetting setting = new EngineSetting(
                        rootFolderName + "/" + fileName, // folder
                        listener// listener
                );
                setting.setASAPEngine(engine);
                this.folderMap.put(engine.format, setting);
            }
        }
    }

    /**
     * increase era for each engine
     */
    public void newEra() throws IOException, ASAPException {
        for(CharSequence format : this.folderMap.keySet()) {
            ASAPEngine engine = this.getEngineByFormat(format);
            engine.newEra();
        }
    }

    @Override
    public void setASAPChunkReceivedListener(CharSequence appName, ASAPChunkReceivedListener listener)
            throws ASAPException {

        EngineSetting engineSetting = this.folderMap.get(appName);
        if(engineSetting == null) {
            throw new ASAPException("there is no ASAPEngine for app " + appName);
        }

        engineSetting.listener = listener;
    }

    @Override
    public ASAPChunkReceivedListener getListenerByFormat(CharSequence format) throws ASAPException {
        EngineSetting engineSetting = this.folderMap.get(format);
        if(engineSetting == null) throw new ASAPException("unknown format: " + format);

        return engineSetting.listener;
    }

    public CharSequence getOwner() {
        return this.owner;
    }

    public ASAPEngine getEngineByFormat(CharSequence format) throws ASAPException, IOException {
        // get engine
        EngineSetting engineSetting = this.getEngineSettings(format);
        ASAPEngine asapEngine = engineSetting.engine;

        if (asapEngine == null) {
            asapEngine = ASAPEngineFS.getASAPEngine(owner.toString(), engineSetting.folder.toString(), format);
            engineSetting.setASAPEngine(asapEngine); // remember - keep that object
        }
        return asapEngine;
    }

    @Override
    public ASAPEngine getASAPEngine(CharSequence appName, CharSequence format)
            throws IOException, ASAPException {

        String foldername = this.rootFolderName.toString() + "/" + appName.toString();
        // already exists?
        try {
            ASAPEngine existingASAPEngineFS = ASAPEngineFS.getExistingASAPEngineFS(foldername);
            if(existingASAPEngineFS != null) {
                return existingASAPEngineFS;
            }
        }
        catch(ASAPException e) {
            System.out.println(this.getLogStart() + "engine does not yet exist. folder " + foldername);
        }

        System.out.println(this.getLogStart() + "setup engine with folder" + foldername);
        ASAPEngine asapEngine = ASAPEngineFS.getASAPEngine(this.getOwner().toString(), foldername, format);
        // add to folderMap
        EngineSetting setting = new EngineSetting(foldername, this.listener);
        setting.setASAPEngine(asapEngine);
        this.folderMap.put(format, setting);

        return asapEngine;
    }

    private EngineSetting getEngineSettings(CharSequence format) throws ASAPException {
        EngineSetting folderAndListener = folderMap.get(format);
        if(folderAndListener == null)
            throw new ASAPException("no folder for owner / format: " + owner + "/" + format);

        return folderAndListener;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                          connection management                                         //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////// notification
    private List<ASAPConnectionChangedListener> asapConnectionChangedListenerList = new ArrayList<>();

    private void notifyASAPConnectionChanged() {
        for(ASAPConnectionChangedListener l : this.asapConnectionChangedListenerList) {
            l.asapConnectionedPeers(asapOnlineConnectionThread.keySet());
        }
    }

    public void addASAPConnectionChangedListener(ASAPConnectionChangedListener listener) {
        this.asapConnectionChangedListenerList.add(listener);
    }

    public void removeASAPConnectionChangedListener(ASAPConnectionChangedListener listener) {
        this.asapConnectionChangedListenerList.remove(listener);
    }

    ////////////////////////// thread management - just keep track who is running
    /** all running threads */
    private List<Thread> runningThreads = new ArrayList<>();

    private void threadStarted(Thread thread) {
        this.runningThreads.add(thread);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("launched new thread (");
        sb.append(thread.getClass().getSimpleName());
        sb.append(") | #threads: ");
        sb.append(this.runningThreads.size());
        System.out.println(sb.toString());
    }

    @Override
    public synchronized void finished(Thread thread) {
        if(thread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("finished thread cannot be null - do nothing");
            System.err.println(sb.toString());
            return;
        }

        this.runningThreads.remove(thread);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("thread terminated - number of running threads is now: ");
        sb.append(this.runningThreads.size());
        System.out.println(sb.toString());
    }

    // running online connections
    private Map<CharSequence, ASAPOnlineConnection> asapOnlineConnectionThread = new HashMap<>();

    private boolean supportOnline = false;
    public void setSupportOnline(boolean on) {
        this.supportOnline = on;
    }

    //////////////// start a startup session - first greetings and get to know each other
    public void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        // start a startup session first
        ASAPChunkExchangeSession asapStartupConnection = new ASAPChunkExchangeSession(
                is, os, this, new ASAP_Modem_Impl(),
                this.maxExecutionTime, this, this);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("handleConnection: ask any aspStorage to increment era.");
        System.out.println(sb.toString());

        for(CharSequence format : this.folderMap.keySet()) {
            ASAPStorage asapStorage = this.getEngineByFormat(format);
            asapStorage.newEra();
        }

        asapStartupConnection.start();

        // remember
        this.threadStarted(asapStartupConnection);
    }

    @Override
    public void asapStartupConnectionTerminatedWithException(
            ASAPStartupConnection connection, Exception terminatingException) {

        System.out.println(this.getLogStart() + "start session terminated with exception - nothing to do");
    }

    @Override
    public synchronized void asapStartupConnectionTerminated(ASAPStartupConnection connection) {
        if(!this.supportOnline) {
            System.out.println(this.getLogStart() + "startup terminated successfully - no online support, though");
            return;
        }

        System.out.println(this.getLogStart() + "startup terminated successfully - start online session");
        try {
            ASAPOnlineConnection_Impl asapOnlineConnection = new ASAPOnlineConnection_Impl(
                            connection.getPeerID(),
                            connection.getInputStream(),
                            connection.getOutputStream(),
                    this, this);

            System.out.println(this.getLogStart() + "new online connection established to peer:"
                    + connection.getPeerID());

            this.asapOnlineConnectionThread.put(connection.getPeerID(), asapOnlineConnection);

            // start it
            asapOnlineConnection.start();

            // tell the world
            this.notifyASAPConnectionChanged();
        }
        catch(ASAPException e) {
            System.err.println(this.getLogStart() + "could not create asap online connection: "
                    + e.getLocalizedMessage());
        }
    }

    @Override
    public synchronized void asapOnlineConnectionTerminated(ASAPOnlineConnection asapOnlineConnection,
                                                            Exception terminatingException) {

        if(asapOnlineConnection == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("terminated connection cannot be null - do nothing");
            System.err.println(sb.toString());
            return;
        }

        // get asapOnlineConnection name
        CharSequence peerName = asapOnlineConnection.getRemotePeer();
        this.asapOnlineConnectionThread.remove(peerName);

        // logging
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("asapOnlineConnection terminated connected to: ");

        if(peerName != null) {
            sb.append(peerName);
        } else {
            sb.append("null");
        }

        if(terminatingException != null) {
            sb.append(" | with exception: ");
            sb.append(terminatingException);
        } else {
            // we have an open connection - which terminated without an exception
            // start online phase?

        }

        System.out.println(sb.toString());
        this.notifyASAPConnectionChanged();
    }

    @Override
    public boolean existASAPConnection(CharSequence recipient) {
        return this.getASAPOnlineConnection(recipient) != null;
    }

    @Override
    public ASAPOnlineConnection getASAPOnlineConnection(CharSequence recipient) {
        return this.asapOnlineConnectionThread.get(recipient);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                              ASAP management                                           //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void pushInterests(OutputStream os) throws IOException, ASAPException {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        // issue an interest for each owner / format combination
        for(CharSequence format : this.folderMap.keySet()) {
            protocol.interest(this.owner, null, format,null, -1, -1, os, false);
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + "(" + this.owner + "): ";
    }
}
