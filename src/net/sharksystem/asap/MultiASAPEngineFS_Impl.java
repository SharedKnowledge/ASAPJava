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

public class MultiASAPEngineFS_Impl implements MultiASAPEngineFS, ASAPConnectionListener, ThreadFinishedListener {
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

    public ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        ASAPConnection_Impl asapConnection = new ASAPConnection_Impl(
                is, os, this, new ASAP_Modem_Impl(),
                maxExecutionTime, this, this);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("handleConnection: ask any aspStorage to increment era.");
        System.out.println(sb.toString());

        for(CharSequence format : this.folderMap.keySet()) {
            ASAPStorage asapStorage = this.getEngineByFormat(format);
            asapStorage.newEra();
        }

        Thread thread = new Thread(asapConnection);
        thread.start();

        // remember
        this.runningThreads.add(thread);

        sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("launched new asapConnection thread, total number is now: ");
        sb.append(this.runningThreads.size());
        System.out.println(sb.toString());

        return asapConnection;
    }

    /** all running threads */
    private List<Thread> runningThreads = new ArrayList<>();

    @Override
    public void finished(Thread thread) {
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

    // thread connected to a peer
    private Map<CharSequence, ASAPConnection> connectedThreads = new HashMap<>();
    private Map<ASAPConnection, CharSequence> threadPeerNames = new HashMap<>();

    @Override
    public void asapConnectionStarted(String peerName, ASAPConnection thread) {
        if(thread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("asap connection started but thread terminated cannot be null - do nothing");
            System.err.println(sb.toString());
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("asap connection started, got a peername: ");
        sb.append(peerName);
        System.out.println(sb.toString());

        this.connectedThreads.put(peerName, thread);
        this.threadPeerNames.put(thread, peerName);
    }

    @Override
    public synchronized void asapConnectionTerminated(Exception terminatingException, ASAPConnection thread) {
        if(thread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("terminated connection cannot be null - do nothing");
            System.err.println(sb.toString());
            return;
        }

        // get thread name
        CharSequence threadName = this.threadPeerNames.remove(thread);
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("thread terminated connected to: ");

        if(threadName != null) {
            sb.append(threadName);
        } else {
            sb.append("null");
        }

        System.out.println(sb.toString());
    }

    @Override
    public boolean existASAPConnection(CharSequence recipient) {
        return this.getASAPConnection(recipient) != null;
    }

    @Override
    public ASAPConnection getASAPConnection(CharSequence recipient) {
        return this.connectedThreads.get(recipient);
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

    @Override
    public Thread getExecutorThread(ASAP_PDU_1_0 asappdu, InputStream is, OutputStream os,
                                    ThreadFinishedListener threadFinishedListener) throws ASAPException {
        // process pdu
        return new ASAPPDUExecutor(asappdu, is, os,
                this.getEngineSettings(asappdu.getFormat()),
                new ASAP_Modem_Impl());
    }

   private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

}
