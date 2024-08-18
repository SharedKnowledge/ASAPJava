package net.sharksystem.asap.engine;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.asap.crypto.*;
import net.sharksystem.fs.ExtraData;
import net.sharksystem.fs.ExtraDataFS;
import net.sharksystem.utils.*;
import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.listenermanager.management.ASAPManagementMessageHandler;
import net.sharksystem.asap.protocol.*;

import java.io.*;
import java.util.*;

public class ASAPInternalPeerFS implements
        ASAPInternalPeer, ASAPConnectionListener, ThreadFinishedListener, ASAPUndecryptableMessageHandler/*, ASAPChunkReceivedListener */ {

    private static final String DEFAULT_ASAP_MANAGEMENT_ENGINE_ROOTFOLDER = "ASAPManagement";
    private final CharSequence rootFolderName;
    private final ASAPChunkAssimilatedListener listener;
    private CharSequence owner;
    private HashMap<CharSequence, EngineSetting> folderMap;
    private final long maxExecutionTime;
    private ASAPKeyStore ASAPKeyStore;
    private DefaultSecurityAdministrator defaultSecurityAdministrator = new DefaultSecurityAdministrator();
    private InMemoASAPKeyStore inMemoASAPKeyStore;

    public ASAPPoint2PointCryptoSettings getASAPCommunicationCryptoSettings() {
        return this.defaultSecurityAdministrator;
    }

    public void setSecurityAdministrator(DefaultSecurityAdministrator securityAdministrator) {
        this.defaultSecurityAdministrator = securityAdministrator;
    }

    public ASAPCommunicationSetting getASAPCommunicationControl() {
        return this.defaultSecurityAdministrator;
    }

    public static ASAPInternalPeer createASAPPeer(CharSequence owner, CharSequence rootFolder,
                                                  long maxExecutionTime,
                                                  Collection<CharSequence> supportFormats,
                                                  ASAPChunkAssimilatedListener listener)
            throws ASAPException, IOException {

        return new ASAPInternalPeerFS(owner, rootFolder, maxExecutionTime, supportFormats, listener);
    }

    public static ASAPInternalPeer createASAPPeer(CharSequence owner, CharSequence rootFolder,
                                                  Collection<CharSequence> supportFormats,
                                                  ASAPChunkAssimilatedListener listener)
            throws ASAPException, IOException {

        return new ASAPInternalPeerFS(owner, rootFolder, DEFAULT_MAX_PROCESSING_TIME, supportFormats, listener);
    }

    public static ASAPInternalPeer createASAPPeer(CharSequence owner, CharSequence rootFolder,
                                                  long maxExecutionTime,
                                                  ASAPChunkAssimilatedListener listener) throws ASAPException, IOException {
        return new ASAPInternalPeerFS(owner, rootFolder, maxExecutionTime, listener);
    }

    public static ASAPInternalPeer createASAPPeer(CharSequence owner, CharSequence rootFolder,
                                                  ASAPChunkAssimilatedListener listener) throws ASAPException, IOException {
        return new ASAPInternalPeerFS(owner, rootFolder, DEFAULT_MAX_PROCESSING_TIME, listener);
    }

    public static ASAPInternalPeer createASAPPeer(CharSequence folder, ASAPChunkAssimilatedListener listener)
            throws ASAPException, IOException {

        return ASAPInternalPeerFS.createASAPPeer(ASAPEngine.DEFAULT_OWNER, folder,
                DEFAULT_MAX_PROCESSING_TIME, listener);
    }

    /**
     * assumed that a number of asap storages are already exists in subdirectories of the
     * root directory. setting list can be created by iterating those storages.
     * @param rootFolderName
     */
    private ASAPInternalPeerFS(CharSequence owner, CharSequence rootFolderName, long maxExecutionTime,
                               ASAPChunkAssimilatedListener listener) throws ASAPException, IOException {
        this(owner, rootFolderName, maxExecutionTime, null, listener);
    }

    private ASAPInternalPeerFS(CharSequence owner, CharSequence rootFolderName, long maxExecutionTime,
                               Collection<CharSequence> apps, ASAPChunkAssimilatedListener listener)
            throws ASAPException, IOException {

        // owner id must not be a numerical value only - it would interfere with our era numbers
        try {
            Integer.parseInt(owner.toString());
            throw new ASAPException("peer id must not only be a numeric number like 42. " +
                    "It can be ArthurDent_42, though: " + owner);
        }
        catch(NumberFormatException e) {
            // that's a good thing - id is not only a number
        }

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
        /*
        if(!this.isASAPManagementEngineRunning()) {
            Log.writeLog(this, "no asap management engine yet - set it up.");

            this.setupEngine(DEFAULT_ASAP_MANAGEMENT_ENGINE_ROOTFOLDER, ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
        }
         */

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
                    this.setupEngine(Utils.url2FileName(appFormat.toString()), appFormat);
                }
            }
        }

        //this.restoreExtraData();

//        Log.writeLog(this, "SHOULD also set up engine " + FORMAT_UNDECRYPTABLE_MESSAGES);
    }

    private void setupEngine(CharSequence folderName, CharSequence formatName) throws IOException, ASAPException {
        String fileName = this.rootFolderName + "/" + folderName;
        Log.writeLog(this, "set up: " + formatName + " in folder " + fileName);
        ASAPEngine asapEngine = ASAPEngineFS.getASAPStorage(this.getOwner().toString(),
                fileName, formatName);

        asapEngine.setSecurityAdministrator(this.defaultSecurityAdministrator);

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

        Log.writeLog(this, "setting up ASAPEngine based on sub folders in " + this.rootFolderName);
        File[] files = rootFolder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String fileName = file.getCanonicalPath();
                Log.writeLog(this, "setup engine for " + fileName);
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
    public void setASAPChunkReceivedListener(CharSequence appName, ASAPChunkAssimilatedListener listener)
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
        asapEngine.setSecurityAdministrator(this.defaultSecurityAdministrator);
        return asapEngine;
    }

    public boolean asapRoutingAllowed(CharSequence applicationFormat) throws IOException, ASAPException {
        return this.getEngineByFormat(applicationFormat).routingAllowed();
    }

    public void setAsapRoutingAllowed(CharSequence applicationFormat, boolean allowed)
            throws IOException, ASAPException {

        this.getEngineByFormat(applicationFormat).setBehaviourAllowRouting(allowed);
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

        asapEngine.setSecurityAdministrator(this.defaultSecurityAdministrator);

        return asapEngine;
    }

    @Override
    public ASAPChunkAssimilatedListener getListenerByFormat(CharSequence format) throws ASAPException {
        EngineSetting engineSetting = this.folderMap.get(format);
        if(engineSetting == null) throw new ASAPException("unknown format: " + format);

        return engineSetting.listener;
    }

    private String getEngineFolderByAppName(CharSequence appName) {
        return this.rootFolderName.toString() + "/" + Utils.url2FileName(appName.toString());
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
            Log.writeLog(this, "engine does not yet exist. folder " + foldername);
        }

        Log.writeLog(this, "setup engine with folder" + foldername);
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
            return this.handleConnection(is, os, false, false);
    }

    public ASAPConnection handleConnection(InputStream is, OutputStream os,
                                           boolean encrypt, boolean sign) throws IOException, ASAPException {

        return this.handleConnection(is, os,false, false,null, null);
    }

    public ASAPConnection handleConnection(InputStream is, OutputStream os, ASAPEncounterConnectionType connectionType)
            throws IOException, ASAPException {

        return this.handleConnection(is, os,false, false, connectionType, null, null);
    }

    /**
     *
     * @deprecated need connection type
     */
    public ASAPConnection handleConnection(
            InputStream is, OutputStream os, boolean encrypt, boolean sign,
            Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList
    ) throws IOException, ASAPException {

        return this.handleConnection(is, os, encrypt, sign,
                ASAPEncounterConnectionType.UNKNOWN, appsWhiteList, appsBlackList);
    }

    public ASAPConnection handleConnection(
            InputStream is, OutputStream os, boolean encrypt, boolean sign, ASAPEncounterConnectionType connectionType,
            Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList) throws IOException, ASAPException {

        if(appsWhiteList == null) appsWhiteList = new HashSet<>(); // empty set no null
        if(appsBlackList == null) appsBlackList = new HashSet<>(); // empty set no null

        // TODO add white / black list.
        ASAPPersistentConnection asapConnection = new ASAPPersistentConnection(
                is, os, this, new ASAP_Modem_Impl(),
                this, this.ASAPKeyStore,
                maxExecutionTime, this, this, encrypt, sign, connectionType);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("handleConnection");
        Log.writeLog(this, sb.toString());

        this.announceNewEra(); // announce when connection is actually established

        Thread thread = new Thread(asapConnection);
        thread.start();

        // remember
        this.runningThreads.add(thread);

        sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("launched new asapConnection thread, total number is now: ");
        sb.append(this.runningThreads.size());
        Log.writeLog(this, sb.toString());

        return asapConnection;
    }

    public void announceNewEra() throws IOException, ASAPException {
        Log.writeLog(this, "announce new era");
        for(CharSequence format : this.folderMap.keySet()) {
            ASAPInternalStorage asapStorage = this.getEngineByFormat(format);
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
            Log.writeLogErr(this, sb.toString());
            return;
        }

        this.runningThreads.remove(thread);

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("thread terminated - number of running threads is now: ");
        sb.append(this.runningThreads.size());
        Log.writeLog(this, sb.toString());
    }

    // threads connected to a peer
    private Map<CharSequence, ASAPConnection> connectedThreads = new HashMap<>();
    private Map<ASAPConnection, CharSequence> threadPeerNames = new HashMap<>();

    private List<ASAPInternalOnlinePeersChangedListener> onlinePeersChangedListeners = new ArrayList<>();
    public void addOnlinePeersChangedListener(ASAPInternalOnlinePeersChangedListener listener) {
        this.onlinePeersChangedListeners.add(listener);
    }

    public void removeOnlinePeersChangedListener(ASAPInternalOnlinePeersChangedListener listener) {
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
            Log.writeLog(this,
                    "#online peers: " + this.connectedThreads.keySet().size()
                    + " | " + SerializationHelper.collection2String(this.connectedThreads.keySet()));
        } else {
            Log.writeLog(this, "no (more) peers: ");
        }

        if(this.onlinePeersChangedListeners != null) {
            for(ASAPInternalOnlinePeersChangedListener listener: this.onlinePeersChangedListeners) {
                listener.notifyOnlinePeersChanged(this);
            }
        } else {
            Log.writeLog(this, "online peer changed but no listener");
        }
    }

    public Set<CharSequence> getOnlinePeers() {
        if(!this.connectedThreads.isEmpty()) {
            Log.writeLog(this,
                    "getOnlinePeers called | #online peers: " + this.connectedThreads.keySet().size()
                    + " | " + SerializationHelper.collection2String(this.connectedThreads.keySet()));
        } else {
            Log.writeLog(this, "getOnlinePeers called | no (more) peers: ");
        }

        return this.connectedThreads.keySet();
    }

    @Override
    public void asapConnectionStarted(String remotePeerName, ASAPConnection thread) {
        if(thread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("asap connection started but thread terminated cannot be null - do nothing");
            Log.writeLogErr(this,sb.toString());
            return;
        }

        /* we must do it handleConnection - this method is called when we are about processing the first interest pdu
        try {
            this.announceNewEra();
        } catch (IOException | ASAPException e) {
            Log.writeLogErr(this, "could not announce new era: " + e.getLocalizedMessage());
        }
         */

        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("asap connection started, got a peername: ");
        sb.append(remotePeerName);
        Log.writeLog(this, sb.toString());

        this.connectedThreads.put(remotePeerName, thread);
        this.threadPeerNames.put(thread, remotePeerName);
        this.notifyOnlinePeersChangedListener();
    }

    @Override
    public synchronized void asapConnectionTerminated(Exception terminatingException, ASAPConnection thread) {
        if(thread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getLogStart());
            sb.append("terminated connection cannot be null - do nothing");
            Log.writeLogErr(this, sb.toString());
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

        Log.writeLog(this, sb.toString());

        if(peerName != null) {
            try {
                this.announceNewEra();
            } catch (IOException | ASAPException e) {
                Log.writeLogErr(this,"error when announcing new era: " + e.getLocalizedMessage());
            }

            this.notifyOnlinePeersChangedListener();
        } else {
            Log.writeLog(this,
                    "asap connection terminated connected to nobody: don't change era / don't notify listeners");
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
        Log.writeLog(this, "send interest on " + ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
        protocol.interest(this.owner, null, ASAP_1_0.ASAP_MANAGEMENT_FORMAT,null, -1, -1, os, false);
*/
        if(this.folderMap.size() > 0) {
            Log.writeLog(this, "start sending interest for apps/formats");
        } else {
            Log.writeLog(this, "no more apps/formats on that engine - no interests to be sent");
        }

        // management messages must be sent first - if any
        try {
            // exists?
            ASAPEngine managementEngine = this.getEngineByFormat(ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            Log.writeLog(this, "send interest for app/format: " + ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            protocol.interest(this.owner, null,
                    ASAP_1_0.ASAP_MANAGEMENT_FORMAT,
                    null, ASAP_1_0.ERA_NOT_DEFINED, ASAP_1_0.ERA_NOT_DEFINED,
                    os,
                    this.getASAPCommunicationCryptoSettings());
        }
        catch(Exception e) {
            // ignore - engine does not exist
        }
        // issue an interest for each owner / format combination
        for(CharSequence format : this.folderMap.keySet()) {
            if(format.toString().equalsIgnoreCase(ASAP_1_0.ASAP_MANAGEMENT_FORMAT)) continue; // already sent
            ASAPEngine engine = this.getEngineByFormat(format);

            engine.sendInterest(this.owner, protocol, os);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                            Online management                                           //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void activateOnlineMessages() {
//        ASAPOnlineMessageSender asapOnlineMessageSender = new ASAPOnlineMessageSenderEngineSide(this);

        // iterate engines
        for(ASAPEngine engine : this.getEngines()) {
//            engine.attachASAPMessageAddListener(asapOnlineMessageSender);
            engine.activateOnlineMessages(this);
        }
    }

    @Override
    public void deactivateOnlineMessages() {
        // iterate engines
        for(ASAPEngine engine : this.getEngines()) {
            engine.deactivateOnlineMessages();
        }
    }


    @Override
    public void setASAPBasicKeyStorage(ASAPKeyStore ASAPKeyStore) {
        this.ASAPKeyStore = ASAPKeyStore;
    }

    public void sendTransientASAPAssimilateMessage(CharSequence format, CharSequence uri, byte[] messageAsBytes)
            throws IOException, ASAPException {
        this.sendTransientASAPAssimilateMessage(format, uri, (Set<CharSequence>)null, messageAsBytes);
    }

    public void sendTransientASAPAssimilateMessage(CharSequence format, CharSequence uri,
                                                   Set<CharSequence> nextHopPeerIDs, byte[] messageAsBytes) throws IOException, ASAPException {

        this.sendOnlineASAPAssimilateMessage(format, uri, ASAP.TRANSIENT_ERA, nextHopPeerIDs, messageAsBytes);
    }

    public void sendTransientASAPAssimilateMessage(CharSequence format, CharSequence uri,
                       CharSequence nextHopPeerID, byte[] messageAsBytes) throws IOException, ASAPException {

        if(nextHopPeerID == null) throw new ASAPException("next hop peer id must not be null");

        if(!this.existASAPConnection(nextHopPeerID)) {
            String log = "cannot send transient message. No open connection to peer with id: " + nextHopPeerID;
            Log.writeLog(this, log);
            throw new ASAPException(log);
        }

        Set<CharSequence> nextHopPeerIDs = new HashSet<>();
        nextHopPeerIDs.add(nextHopPeerID);
        this.sendOnlineASAPAssimilateMessage(format, uri, ASAP.TRANSIENT_ERA, nextHopPeerIDs, messageAsBytes);
    }

    public void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence uri, int era, byte[] messageAsBytes)
            throws IOException, ASAPException {

        this.sendOnlineASAPAssimilateMessage(format, uri, era, null, messageAsBytes);
    }

    public void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence uri, int era,
                     Set<CharSequence> receiver, byte[] messageAsBytes) throws IOException, ASAPException {
        // setup online message sender thread
        Log.writeLog(this, "setup online message sender object");
        ASAPOnlineMessageSender asapOnlineMessageSender = new ASAPOnlineMessageSenderEngineSide(this);
        Log.writeLog(this, "call send asap assimilate message with online message sender");
        asapOnlineMessageSender.sendASAPAssimilateMessage(format, uri, receiver, messageAsBytes, ASAP.TRANSIENT_ERA);
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

    //////////////////////////////// handle message this peer cannot decrypt
    @Override
    public void handleUndecryptableMessage(
            ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage,
            CharSequence receiver) {

        Log.writeLog(this, "handle undecryptable messages from " + receiver);

        try {
            ASAPEngine undecryptEngine =
                    this.getASAPEngine(ASAPUndecryptableMessageHandler.FORMAT_UNDECRYPTABLE_MESSAGES);

            undecryptEngine.add(
                    URI_UNDECRYPTABLE_MESSAGES,
                    ASAPCryptoAlgorithms.getEncryptedMessagePackageAsBytes(encryptedMessagePackage));
        } catch (IOException | ASAPException e) {
            Log.writeLog(this, "cannot handle undecrypted messages - no engine present");
        }
    }

    ///////////////////////////////// SharkNet
    @Override
    public ASAPKeyStore getASAPKeyStore() throws ASAPSecurityException {
        if(this.inMemoASAPKeyStore == null) {
            this.inMemoASAPKeyStore = new InMemoASAPKeyStore(this.getOwner().toString());
        }
        return this.inMemoASAPKeyStore;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                        make extra data persist                                        //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ExtraData extraData = null;

    private ExtraData getExtraData() throws SharkException, IOException {
        if(this.extraData == null) {
            this.extraData = new ExtraDataFS(this.rootFolderName);
        }

        return this.extraData;
    }

    public void putExtra(CharSequence key, byte[] value) throws IOException, SharkException {
        this.getExtraData().putExtra(key, value);
    }

    @Override
    public void putExtra(CharSequence key, Integer value) throws IOException, SharkException {
        this.getExtraData().putExtra(key, value);
    }

    @Override
    public void putExtra(CharSequence key, String value) throws IOException, SharkException {
        this.getExtraData().putExtra(key, value);
    }

    @Override
    public void putExtra(CharSequence key, Set<CharSequence> value) throws IOException, SharkException {
        this.getExtraData().putExtra(key, value);
    }

    public byte[] getExtra(CharSequence key) throws IOException, SharkException {
        return this.getExtraData().getExtra(key);
    }

    @Override
    public int getExtraInteger(CharSequence key) throws IOException, SharkException {
        return this.getExtraData().getExtraInteger(key);
    }

    @Override
    public Set<CharSequence> getExtraCharSequenceSetParameter(CharSequence key) throws IOException, SharkException {
        return this.getExtraData().getExtraCharSequenceSetParameter(key);
    }

    @Override
    public String getExtraString(CharSequence key) throws IOException, SharkException {
        return this.getExtraData().getExtraString(key);
    }

    @Override
    public void removeAll() throws IOException, SharkException {
        this.getExtraData().removeAll();
    }
}
