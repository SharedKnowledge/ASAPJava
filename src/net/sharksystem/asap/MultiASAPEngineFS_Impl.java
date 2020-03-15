package net.sharksystem.asap;

import net.sharksystem.asap.management.ASAPManagementCreateASAPStorageMessage;
import net.sharksystem.asap.management.ASAPManagementMessage;
import net.sharksystem.asap.management.ASAPManagementMessageHandler;
import net.sharksystem.asap.management.ASAPManagementStorage;
import net.sharksystem.asap.protocol.*;
import net.sharksystem.asap.util.Helper;
import net.sharksystem.asap.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class MultiASAPEngineFS_Impl implements
        MultiASAPEngineFS, ASAPConnectionListener, ThreadFinishedListener/*, ASAPChunkReceivedListener */ {

    private static final String DEFAULT_ASAP_MANAGEMENT_ENGINE_ROOTFOLDER = "ASAPManagement";
    private final CharSequence rootFolderName;
    private final ASAPChunkReceivedListener listener;
    private CharSequence owner;
    private HashMap<CharSequence, EngineSetting> folderMap;
    private final long maxExecutionTime;

    public static MultiASAPEngineFS createMultiEngine(CharSequence owner, CharSequence rootFolder,
                                                      long maxExecutionTime,
                                                      Collection<CharSequence> supportFormats,
                                                      ASAPChunkReceivedListener listener)
                            throws ASAPException, IOException {

        return new MultiASAPEngineFS_Impl(owner, rootFolder, maxExecutionTime, supportFormats, listener);
    }

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
    private MultiASAPEngineFS_Impl(CharSequence owner, CharSequence rootFolderName, long maxExecutionTime,
                                   ASAPChunkReceivedListener listener) throws ASAPException, IOException {
        this(owner, rootFolderName, maxExecutionTime, null, listener);
    }

    private MultiASAPEngineFS_Impl(CharSequence owner, CharSequence rootFolderName, long maxExecutionTime,
        Collection<CharSequence> apps, ASAPChunkReceivedListener listener) throws ASAPException, IOException {
        this.owner = owner;
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

        /////////////////// asap management app //////////////////////////////////////////////////////
        // check if management engine running
        if(!this.isASAPManagementEngineRunning()) {
            System.out.println(this.getLogStart() + "no asap management engine yet - set it up.");

            this.setupEngine(DEFAULT_ASAP_MANAGEMENT_ENGINE_ROOTFOLDER, ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            /*
            String fileName = rootFolderName + "/" + DEFAULT_ASAP_MANAGEMENT_ENGINE_ROOTFOLDER;
            ASAPEngine asapManagementEngine = ASAPEngineFS.getASAPStorage(this.getOwner().toString(),
                    fileName, ASAP_1_0.ASAP_MANAGEMENT_FORMAT);

            EngineSetting setting = new EngineSetting(
                    fileName, // folder
                    listener// listener
            );
            setting.setASAPEngine(asapManagementEngine);
            this.folderMap.put(ASAP_1_0.ASAP_MANAGEMENT_FORMAT, setting);
            */
        }

        // set listener to asap management app
        EngineSetting folderAndListener = folderMap.get(ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
        if(folderAndListener != null) {
            folderAndListener.listener = new ASAPManagementMessageHandler(this);
        }

        /////////////////// not yet created engines////////////////////////////////////////////////////
        if(apps != null) {
            for (CharSequence appFormat : apps) {
                // check if exists
                try {
                    this.getEngineByFormat(appFormat);
                } catch (ASAPException e) {
                    // set it up
                    this.setupEngine(appFormat, appFormat);
                }
            }
        }
    }

    private void setupEngine(CharSequence folderName, CharSequence formatName) throws IOException, ASAPException {
        String fileName = this.rootFolderName + "/" + folderName;
        System.out.println(this.getLogStart() + "set up: " + formatName + " in folder " + fileName);
        ASAPEngine asapEngine = ASAPEngineFS.getASAPStorage(this.getOwner().toString(),
                fileName, formatName);

        EngineSetting setting = new EngineSetting(
                fileName, // folder
                this.listener// listener
        );
        setting.setASAPEngine(asapEngine);
        this.folderMap.put(formatName, setting);
    }

    private void setupFolderMap() throws IOException, ASAPException {
        this.folderMap = new HashMap<>();
        File rootFolder = new File(rootFolderName.toString());

        System.out.println(this.getLogStart() + "setting up ASAPEngine based on subfolders in " + this.rootFolderName);
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
            throw new ASAPException("there is no ASAPEngine for app/format " + appName);
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
    public ASAPEngine createEngineByFormat(CharSequence format) throws ASAPException, IOException {
        try {
            return this.getEngineByFormat(format);
        }
        catch(ASAPException e) {
            // does not exist yet
        }

        String folderName = this.getEngineFolderByAppName(format);
        ASAPEngine asapEngine = ASAPEngineFS.getASAPEngine(String.valueOf(this.getOwner()), folderName, format);
        this.folderMap.put(format, new EngineSetting(folderName, listener));

        return asapEngine;
    }

    @Override
    public ASAPChunkReceivedListener getListenerByFormat(CharSequence format) throws ASAPException {
        EngineSetting engineSetting = this.folderMap.get(format);
        if(engineSetting == null) throw new ASAPException("unknown format: " + format);

        return engineSetting.listener;
    }

    private String getEngineFolderByAppName(CharSequence appName) {
        return this.rootFolderName.toString() + "/" + appName;
    }

    @Override
    public ASAPEngine getASAPEngine(CharSequence format)
            throws IOException, ASAPException {

        String foldername = this.getEngineFolderByAppName(format);
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

    public EngineSetting getEngineSettings(CharSequence format) throws ASAPException {
        EngineSetting folderAndListener = folderMap.get(format);
        if(folderAndListener == null)
            throw new ASAPException("no folder for owner / format: " + owner + "/" + format);

        return folderAndListener;
    }

    public Set<CharSequence> getFormats() {
        return this.folderMap.keySet();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                          connection management                                         //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        ASAPPersistentConnection asapConnection = new ASAPPersistentConnection(
                is, os, this, new ASAP_Modem_Impl(),
                maxExecutionTime, this, this);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("handleConnection: ask any asapStorage to increment era.");
        System.out.println(sb.toString());

        // this.announceNewEra(); announce when connection is actually established

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

    public void announceNewEra() throws IOException, ASAPException {
        System.out.println(this.getLogStart() + "announce new era");
        for(CharSequence format : this.folderMap.keySet()) {
            ASAPStorage asapStorage = this.getEngineByFormat(format);
            asapStorage.newEra();
        }
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

    // threads connected to a peer
    private Map<CharSequence, ASAPConnection> connectedThreads = new HashMap<>();
    private Map<ASAPConnection, CharSequence> threadPeerNames = new HashMap<>();

    private List<ASAPOnlinePeersChangedListener> onlinePeersChangedListeners = new ArrayList<>();
    public void addOnlinePeersChangedListener(ASAPOnlinePeersChangedListener listener) {
        this.onlinePeersChangedListeners.add(listener);
    }

    public void removeOnlinePeersChangedListener(ASAPOnlinePeersChangedListener listener) {
        this.onlinePeersChangedListeners.remove(listener);
    }

    @Override
    public boolean isASAPManagementEngineRunning() {
        try {
            ASAPEngine engineByFormat = this.getEngineByFormat(ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            if(engineByFormat == null) {
                return false;
            }
        } catch (ASAPException | IOException e) {
            return false;
        }

        return true;
    }

    private void notifyOnlinePeersChangedListener() {
        if(!this.connectedThreads.isEmpty()) {
            System.out.println(this.getLogStart()
                    + "#online peers: " + this.connectedThreads.keySet().size()
                    + " | " + Helper.collection2String(this.connectedThreads.keySet()));
        } else {
            System.out.println(this.getLogStart() + "no (more) peers: ");
        }

        if(this.onlinePeersChangedListeners != null) {
            for(ASAPOnlinePeersChangedListener listener: this.onlinePeersChangedListeners) {
                listener.onlinePeersChanged(this);
            }
        }
    }

    public Set<CharSequence> getOnlinePeers() {
        if(!this.connectedThreads.isEmpty()) {
            System.out.println(this.getLogStart()
                    + "getOnlinePeers called | #online peers: " + this.connectedThreads.keySet().size()
                    + " | " + Helper.collection2String(this.connectedThreads.keySet()));
        } else {
            System.out.println(this.getLogStart() + "getOnlinePeers called | no (more) peers: ");
        }

        return this.connectedThreads.keySet();
    }

    @Override
    public void asapConnectionStarted(String peerName, ASAPConnection thread) {
        if(thread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("asap connection started but thread terminated cannot be null - do nothing");
            System.err.println(sb.toString());
            return;
        }

        try {
            this.announceNewEra();
        } catch (IOException | ASAPException e) {
            System.err.println(this.getLogStart() + "could not announce new era: " + e.getLocalizedMessage());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("asap connection started, got a peername: ");
        sb.append(peerName);
        System.out.println(sb.toString());

        this.connectedThreads.put(peerName, thread);
        this.threadPeerNames.put(thread, peerName);
        this.notifyOnlinePeersChangedListener();
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
        CharSequence peerName = this.threadPeerNames.remove(thread);
        this.connectedThreads.remove(peerName);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("thread terminated connected to: ");

        if(peerName != null) {
            sb.append(peerName);
        } else {
            sb.append("null");
        }

        System.out.println(sb.toString());

        if(peerName != null) {
            try {
                this.announceNewEra();
            } catch (IOException | ASAPException e) {
                System.err.println(this.getLogStart() + "error when announcing new era: " + e.getLocalizedMessage());
            }

            this.notifyOnlinePeersChangedListener();
        } else {
            System.out.println(this.getLogStart()
                    + "asap connection terminated connected to nobody: don't change era / don't notify listeners");
        }
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
/*
        // in any case: issue an interest for management information first
        System.out.println(this.getLogStart() + "send interest on " + ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
        protocol.interest(this.owner, null, ASAP_1_0.ASAP_MANAGEMENT_FORMAT,null, -1, -1, os, false);
*/
        if(this.folderMap.size() > 0) {
            System.out.println(this.getLogStart() + "start sending interest for apps/formats");
        } else {
            System.out.println(this.getLogStart() + "no more apps/formats on that engine - no interests to be sent");
        }

        // management messages must be sent first - if any
        try {
            ASAPEngine managementEngine = this.getEngineByFormat(ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            System.out.println(this.getLogStart() + "send interest for app/format: " + ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            protocol.interest(this.owner, null, ASAP_1_0.ASAP_MANAGEMENT_FORMAT,null, -1, -1, os, false);
        }
        catch(Exception e) {
            // ignore - engine does not exist
        }
        // issue an interest for each owner / format combination
        for(CharSequence format : this.folderMap.keySet()) {
            if(format.toString().equalsIgnoreCase(ASAP_1_0.ASAP_MANAGEMENT_FORMAT)) continue; // already sent
            System.out.println(this.getLogStart() + "send interest for app/format: " + format);
            protocol.interest(this.owner, null, format,null, -1, -1, os, false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                            Online management                                           //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void activateOnlineMessages() {
        ASAPOnlineMessageSender asapOnlineMessageSender = new ASAPOnlineMessageSenderEngineSide(this);

        // iterate engines
        for(ASAPEngine engine : this.getEngines()) {
            engine.attachASAPMessageAddListener(asapOnlineMessageSender);
        }
    }

    @Override
    public void deactivateOnlineMessages() {
        // iterate engines
        for(ASAPEngine engine : this.getEngines()) {
            engine.detachASAPMessageAddListener();
        }
    }

    public void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence urlTarget,
            Set<CharSequence> recipients, byte[] messageAsBytes, int era) throws IOException, ASAPException {

        // setup online message sender thread
        Log.writeLog(this, "setup online message sender object");
        ASAPOnlineMessageSender asapOnlineMessageSender = new ASAPOnlineMessageSenderEngineSide(this);
        Log.writeLog(this, "call send asap assimilate message with online message sender");
        asapOnlineMessageSender.sendASAPAssimilateMessage(format, urlTarget, recipients, messageAsBytes, era);
    }

    private Collection<ASAPEngine> getEngines() {
        Collection<ASAPEngine> engineList = new ArrayList<>();

        if(this.folderMap.values() != null) {
            for (EngineSetting engineSetting : this.folderMap.values()) {
                engineList.add(engineSetting.engine);
            }
        }

        return engineList;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() /* + "(" + this + ")" */ + "(" + this.getOwner() + "): ";
    }
}
