package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPHopImpl;
import net.sharksystem.asap.EncounterConnectionType;
import net.sharksystem.asap.*;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.listenermanager.management.ASAPManagementStorage;
import net.sharksystem.asap.listenermanager.management.ASAPManagementStorageImpl;
import net.sharksystem.asap.protocol.*;
import net.sharksystem.asap.utils.PeerIDHelper;
import net.sharksystem.utils.Log;
import net.sharksystem.asap.crypto.ASAPPoint2PointCryptoSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * That ASAPEngine manages exchange of stored messages with peers.
 * See ASPChunkStorage for details.
 * 
 * @see ASAPInternalStorage
 * @author thsc
 */
public abstract class ASAPEngine extends ASAPStorageImpl implements ASAPInternalStorage, ASAPProtocolEngine, ASAPManagementStorage {
    private DefaultSecurityAdministrator securityAdministrator;

    public void setSecurityAdministrator(DefaultSecurityAdministrator securityAdministrator) {
        this.securityAdministrator = securityAdministrator;
    }

    public static final String ANONYMOUS_OWNER = "anon";
    static String DEFAULT_OWNER = ANONYMOUS_OWNER;
    static int DEFAULT_INIT_ERA = 0;

    protected String owner = ANONYMOUS_OWNER;

    protected int era = ASAP.INITIAL_ERA;
    protected int oldestEra = ASAP.INITIAL_ERA;
    protected HashMap<String, Integer> lastSeen = new HashMap<>();
    protected ASAPMemento memento = null;
    public long lastMementoWritten;

    protected boolean dropDeliveredChunks = false;

    private ASAPOnlineMessageSender asapOnlineMessageSender;
    protected boolean contentChanged = false;
    protected boolean routingAllowed = true;

    protected ASAPEngine(ASAPChunkStorage chunkStorage, CharSequence chunkContentFormat)
            throws ASAPException, IOException {
        //super(chunkStorage, chunkContentFormat);

        this.chunkStorage = chunkStorage;
        if(chunkContentFormat != null) {
            this.format = chunkContentFormat.toString();
        } else {
            throw new ASAPException("format expected. like application/x-sn2-makan");
        }
    }

    private void saveStatus() throws IOException {
        if (this.memento != null) {
            this.memento.save(this);
        }
    }

    CryptoControl getCryptoControl() {
        return this.securityAdministrator;
    }

    public ASAPEnginePermissionSettings getASAPEnginePermissionSettings() {
        return this.securityAdministrator;
    }

    public ASAPCommunicationSetting getASAPCommunicationControl() {
        return this.securityAdministrator;
    }

    public ASAPPoint2PointCryptoSettings getASAPCommunicationCryptoSettings() {
        return this.securityAdministrator;
    }

    public void attachASAPMessageAddListener(ASAPOnlineMessageSender asapOnlineMessageSender) {
        this.asapOnlineMessageSender = asapOnlineMessageSender;
    }

    public void detachASAPMessageAddListener() {
        this.asapOnlineMessageSender = null;
    }

    /**
     * We interpret an existing chunk with *no* recipients as
     * public chunk
     * @param chunk
     * @return
     */
    private boolean isPublic(ASAPInternalChunk chunk) {
        return chunk.getRecipients().isEmpty();
    }

    abstract void syncMemento() throws IOException;

    @Override
    public void newEra() {
        this.newEra(false, -1);
    }

    private void newEra(boolean force, int nextEra) {
        try {
            this.syncMemento();
        } catch (IOException e) {
            Log.writeLogErr(this, this.toString(),"cannot read memento: " + e.getLocalizedMessage());
        }

        if(force || this.contentChanged) {
            if(this.contentChanged) Log.writeLog(this, this.toString(), "content changed - increment era...");
            if(force) Log.writeLog(this, this.toString(), "increment era to add new chunks");
            try {
                int oldEra = this.era;
                nextEra = nextEra < 0 ? this.getNextEra(this.era) : nextEra;

                // set as fast as possible to make race conditions less likely
                this.contentChanged = false;

                // we are done here - we are in a new era.
                this.era = nextEra;

                // persistent values
                if(this.memento != null) this.memento.save(this);

                // drop very very old chunks - if available
                this.getChunkStorage().dropChunks(nextEra);

                // setup new era - copy all chunks
                for(ASAPInternalChunk chunk : this.getChunkStorage().getChunks(oldEra)) {
                    ASAPInternalChunk copyChunk = this.getChunkStorage().getChunk(chunk.getUri(), nextEra);
                    copyChunk.clone(chunk);
                }

                Log.writeLog(this, this.toString(), "era incremented");
            } catch (IOException ex) {
                Log.writeLogErr(this, this.toString(),
                        "IOException while incrementing era: " + ex.getLocalizedMessage());
            }
        } else {
            Log.writeLog(this, this.toString(), "content not changed - era not changed");
        }
    }

    //////////////////////////////////////////////////////////////////////
    //                               Writer                             //
    //////////////////////////////////////////////////////////////////////

    @Override
    public void createChannel(CharSequence uri, Collection<CharSequence> recipients) throws IOException, ASAPException {
        this.createChannel(this.getOwner(), uri, recipients);
    }

    @Override
    public void createChannel(CharSequence owner, CharSequence uri, Collection<CharSequence> recipients)
            throws IOException, ASAPException {

        this.setRecipients(uri, recipients);
        this.getASAPChannelImpl(uri).setOwner(owner);

        // inform recipients about that event
        if(this.isASAPManagementStorageSet()) {
            this.getASAPManagementStorage().notifyChannelCreated(this.format, owner, uri, recipients);
        } else {
            Log.writeLog(this, this.toString(), "asap management storage not set - no propagation of channel creation");
        }
    }

    public void createChannel(CharSequence urlTarget) throws IOException, ASAPException {
        this.createChannel(urlTarget, (CharSequence) null);
    }

    @Override
    public void createChannel(CharSequence urlTarget, CharSequence recipient) throws IOException, ASAPException {
        Set<CharSequence> recipients = new HashSet<>();
        recipients.add(recipient);
        this.createChannel(urlTarget, recipients);
    }

    private ASAPManagementStorage asapManagementStorage = null;
    public void setASAPManagementStorage(ASAPManagementStorage asapManagementStorage) {
        this.asapManagementStorage = asapManagementStorage;
    }

    public ASAPManagementStorage getASAPManagementStorage() throws IOException, ASAPException {
        if(this.asapManagementStorage == null) {
            throw new ASAPException("ASAP Management Storage not set");
        }

        return this.asapManagementStorage;
    }

    public boolean isASAPManagementStorageSet() {
        return this.asapManagementStorage != null;
    }

    public void notifyChannelCreated(CharSequence appName, CharSequence owner,
                                     CharSequence uri, Collection<CharSequence> recipients)
            throws ASAPException, IOException {

        new ASAPManagementStorageImpl(this).notifyChannelCreated(appName, owner, uri, recipients);
    }

    public void addRecipient(CharSequence urlTarget, CharSequence recipient) throws IOException {
        this.chunkStorage.getChunk(urlTarget, this.era).addRecipient(recipient);
    }

    public void setRecipients(CharSequence urlTarget, Collection<CharSequence> recipients) throws IOException {
        this.chunkStorage.getChunk(urlTarget, this.era).setRecipients(recipients);
    }

    public Set<CharSequence> getRecipients(CharSequence urlTarget) throws IOException {
        return this.chunkStorage.getChunk(urlTarget, this.era).getRecipients();
    }

    public void removeRecipient(CharSequence urlTarget, CharSequence recipient) throws IOException {
        this.chunkStorage.getChunk(urlTarget, this.era).removeRecipient(recipient);
    }

    @Override
    public void add(CharSequence uri, CharSequence message) throws IOException {
        this.add(uri, message.toString().getBytes());
    }

    @Override
    public void add(CharSequence uri, byte[] messageAsBytes) throws IOException {
//        Log.writeLog(this, this.toString(), "reached add(uri, byte[] message");
        ASAPInternalChunk chunk = this.chunkStorage.getChunk(uri, this.era);

//        Log.writeLog(this, this.toString(), "call chunk.addMessage()");
        chunk.addMessage(messageAsBytes);

        // remember - something changed in that era
        this.contentChanged();

//        Log.writeLog(this, this.toString(), "online?");
        if(this.asapOnlineMessageSender != null) {
            try {
                Log.writeLog(this, this.toString(), "send online message...");
                this.asapOnlineMessageSender.sendASAPAssimilateMessage(
                        this.format, uri, chunk.getRecipients(),
                        messageAsBytes, this.era);
            } catch (IOException | ASAPException e) {
                Log.writeLog(this, this.toString(),
                    "message written to local storage - but could not write to open asap connection: " +
                    e.getLocalizedMessage());
            }
            Log.writeLog(this, this.toString(), "... done sending online message");
        } else {
            Log.writeLog(this, this.toString(), "online sending not active");
        }
    }

    private void contentChanged() throws IOException {
        this.contentChanged = true;
        Log.writeLog(this, this.toString(), "content changed - save status");
        this.saveStatus();
    }

    private final ASAPChunkStorage chunkStorage;
    protected String format = ASAP_1_0.ANY_FORMAT.toString();

    @Override
    public ASAPChunkStorage getChunkStorage() {
        return this.chunkStorage;
    }


    @Override
    public ASAPInternalChunk createNewChunk(String uri, int newEra) throws IOException {
        ASAPInternalChunk chunk = this.getChunkStorage().getChunk(uri, newEra);
        // set new era
        this.newEra(true, newEra);
        return chunk;
    }

    ASAPChunkStorage getStorage() {
        return this.chunkStorage;
    }

    @Override
    public void putExtra(CharSequence uri, String key, String value) throws IOException {
        this.chunkStorage.getChunk(uri, this.era).putExtra(key, value);
    }

    @Override
    public CharSequence removeExtra(CharSequence uri, String key) throws IOException {
        return this.chunkStorage.getChunk(uri, this.era).removeExtra(key);
    }

    @Override
    public CharSequence getExtra(CharSequence uri, String key) throws IOException {
        return this.chunkStorage.getChunk(uri, this.era).getExtra(key);
    }


    public List<CharSequence> getChannelURIs() throws IOException {
        List<CharSequence> uriList = new ArrayList<>();

        List<ASAPInternalChunk> chunks = this.chunkStorage.getChunks(this.era);
        for(ASAPInternalChunk chunk : chunks) {
            uriList.add(chunk.getUri());
        }

        return uriList;
    }

    @Override
    public ASAPChannel getChannel(CharSequence uri) throws ASAPException, IOException {
        return this.getASAPChannelImpl(uri);
    }

    private ASAPChannelImpl getASAPChannelImpl(CharSequence uri) throws ASAPException, IOException {
        if(this.channelExists(uri)) {
            return new ASAPChannelImpl(this, uri);
        }

        throw new ASAPException("channel with this uri does not exist: " + uri);
    }

    @Override
    public boolean channelExists(CharSequence uri) throws IOException {
        return this.chunkStorage.existsChunk(uri, this.getEra());
    }

    public void removeChannel(CharSequence uri) throws IOException {
        int currentEra;
        int nextEra = this.getOldestEra();
        do {
            currentEra = nextEra;
            ASAPInternalChunk chunk = this.chunkStorage.getChunk(uri, currentEra);
            chunk.drop();
            nextEra = ASAP.nextEra(currentEra);
        } while(currentEra != this.getEra());
    }

    public ASAPMessages getChunkChain(int uriPosition) throws IOException, ASAPException {
        return this.getChunkChain(uriPosition, this.era);
    }

    public ASAPMessages getChunkChain(CharSequence uri, int toEra) throws IOException {
        return this.chunkStorage.getASAPMessages(uri, toEra);
    }

    public ASAPMessages getChunkChain(CharSequence uri) throws IOException {
        return this.getChunkChain(uri, this.getEra());
    }

    public ASAPMessages getChunkChain(int uriPosition, int toEra)
            throws IOException, ASAPException {

        List<CharSequence> channelURIs = this.getChannelURIs();
        if(channelURIs.size() - 1 < uriPosition) {
            throw new ASAPException("position greater than number of channels");
        }

        CharSequence uri = channelURIs.get(uriPosition);
        if(uri == null) {
            throw new ASAPException("uri at postion is null. Position: " + uriPosition);
        }

        return this.chunkStorage.getASAPMessages(uri, toEra);
    }

    //////////////////////////////////////////////////////////////////////
    //                       ProtocolEngine                             //
    //////////////////////////////////////////////////////////////////////

    // extract those algorithms to another class (ASAPDefaultProtocolEngine) ?!
    public void handleASAPAssimilate(ASAP_AssimilationPDU_1_0 asapAssimilationPDU, ASAP_1_0 protocolModem,
         String encounteredPeer, InputStream is, OutputStream os, EncounterConnectionType connectionType,
         ASAPChunkAssimilatedListener listener) throws ASAPException, IOException {

        // before we start - lets crypto
        if(!hasSufficientCrypto(asapAssimilationPDU)) return;

        String senderE2E = asapAssimilationPDU.getSender();
        int eraSender = asapAssimilationPDU.getEra();

        // debug break
        //if(PeerIDHelper.sameID(senderE2E, "Alice_42")) { int i = 42;  }

        if(PeerIDHelper.sameID(senderE2E, this.owner)) {
            Log.writeLogErr(this, this.toString(), "I was offered messages from myself ("
                    + this.owner + ") by " + encounteredPeer + " - refused: ");
            asapAssimilationPDU.takeDataFromStream();
            return;
        }

        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("handle assimilate pdu received from ");
        b.append(senderE2E);
        b.append(" | era: ");
        if(eraSender == ASAP.TRANSIENT_ERA) b.append("transient");
        else b.append(eraSender);
        Log.writeLog(this, this.toString(), b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        boolean changed = false;
        boolean allowedAssimilation = true;

        ASAPInternalChunk incomingChunk = null;
        MessagesContainer messagesContainer = null;
        ASAPInMemoTransientMessages transientMessages = null;

        ASAPHop lastHop = new ASAPHopImpl(encounteredPeer, asapAssimilationPDU.verified(),
                asapAssimilationPDU.encrypted(), connectionType);

        try {
            if(eraSender != ASAP.TRANSIENT_ERA) {
                    incomingChunk = this.getIncomingChunk(encounteredPeer, asapAssimilationPDU);
                    messagesContainer = incomingChunk;
            } else {
                transientMessages =
                        new ASAPInMemoTransientMessages(asapAssimilationPDU, lastHop);

                messagesContainer = transientMessages;
            }
        }
        catch(ASAPException e) {
            asapAssimilationPDU.takeDataFromStream();
            throw e;
        }

        // put messages into container - incoming chunk or transient message container
        List<Integer> messageOffsets = asapAssimilationPDU.getMessageOffsets();

        // iterate messages and stream into chunk
        InputStream protocolInputStream = asapAssimilationPDU.getInputStream();
        Log.writeLog(this, this.toString(), "take data to local chunk or transient message: " + messagesContainer);
        this.streamReceivedMessages2Container(messagesContainer, protocolInputStream,
                messageOffsets, asapAssimilationPDU.getLength());

        // add entry to hop list
        List<ASAPHop> asapHopList = asapAssimilationPDU.getASAPHopList();
        Log.writeLog(this, this.toString(), "got hop list: " + asapHopList);

        // add this new hop
        asapHopList.add(lastHop);

        // add hop list to newly create message container
        messagesContainer.setASAPHopList(asapHopList);

        ////////////////// write log
        String uri = asapAssimilationPDU.getChannelUri();
        // read all messages
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("assimilated: senderE2E: ");
            b.append(senderE2E);
            b.append(", uri: ");
            b.append(uri);
            b.append(", eraSender: ");
            if(eraSender != ASAP.TRANSIENT_ERA) b.append(eraSender);
            else b.append("transient");
            if(listener != null) {
                b.append(" | listener: ");
                b.append(listener.getClass().getSimpleName());
            }
            Log.writeLog(this, this.toString(), b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

        if(listener != null) {
            if(eraSender != ASAP.TRANSIENT_ERA) {
                    listener.chunkStored(this.format,
                        senderE2E,
                        uri,
                        eraSender,
                        asapHopList
                );
            } else {
                listener.transientMessagesReceived(transientMessages, lastHop);
            }

        } else {
            Log.writeLog(this, this.toString(), "no chunk assimilated listener found");
        }
    }
    private ASAPInternalChunk getIncomingChunk(String encounteredPeer, ASAP_AssimilationPDU_1_0 asapAssimilationPDU)
            throws IOException, ASAPException {

        Log.writeLog(this, this.toString(), "called: get incoming chunk");
        String uri = asapAssimilationPDU.getChannelUri();
        int eraSender = asapAssimilationPDU.getEra();
        String senderE2E = asapAssimilationPDU.getSender();
        ASAPInternalStorage incomingStorage = (ASAPInternalStorage) this.getIncomingStorage(senderE2E, true);
        ASAPChunkStorage incomingChunkStorage = incomingStorage.getChunkStorage();
        Log.writeLog(this, this.toString(), "got incoming chunk storage "
                + incomingChunkStorage);

        // get local target for data to come
        if (!incomingChunkStorage.existsChunk(uri, eraSender)) {
            ASAPInternalChunk localChunk = null;
            // is there a local chunk - to clone recipients from?
            if (this.channelExists(uri)) {
                Log.writeLog(this, this.toString(), "get local chunk to copy channel data");
                localChunk = incomingChunkStorage.getChunk(uri, this.getEra()); // get channel information
            } else { // no information that can be copied
                Log.writeLog(this, this.toString(), "asked to set up new channel: (uri/senderE2E): "
                        + uri + " | " + senderE2E);
                // this channel is new to local peer - am I allowed to create it?
                if (!this.securityAdministrator.allowedToCreateChannel(asapAssimilationPDU)) {
                    Log.writeLog(this, this.toString(),
                            ".. not allowed .. TODO not yet implemented .. always set up");

                    //allowedAssimilation = false; // TODO
                } else {
                    Log.writeLog(this, this.toString(), "allowed. Set it up.");
                    this.createChannel(uri);
                }
            }
            Log.writeLog(this, this.toString(), "create new chunk to assimilate received data");
            ASAPInternalChunk incomingChunk = incomingStorage.createNewChunk(uri, eraSender);

            if (localChunk != null) {
                Log.writeLog(this, this.toString(), "copy local meta data into newly created incoming chunk");
                incomingChunk.copyMetaData(this.getChannel(uri));
            }
            Log.writeLog(this, this.toString(), "new incoming chunk created: " + incomingChunk);
            return incomingChunk;
        } else {
            // already exists. Add message if sender == originator and era last era of this channel
            if(incomingStorage.getEra() == eraSender // era is current era
                    && PeerIDHelper.sameID(encounteredPeer, senderE2E) // E2E sender == P2P sender
                    ) {
                Log.writeLog(this, this.toString(),
                        "received chunk exists but sender is originator and current era - message will be added: "
                        + senderE2E + " | " + eraSender + " | " + uri);

                // finish routing...
                if(incomingChunkStorage.getChunk(uri, eraSender).getASAPHopList().size() > 1) {
                    throw new ASAPException("chunk was already routed to me - met originator - will no overwrite chunk");
                }

                return incomingChunkStorage.getChunk(uri, eraSender);
            } else {
                // read assimilation message payload to oblivion!
                throw new ASAPException("chunk that already exists; not current era or not originator: "
                        + senderE2E + " | " + eraSender + " | " + uri);
            }
        }
    }

    private void streamReceivedMessages2Container(MessagesContainer messagesContainer,
          InputStream is, List<Integer> messageOffsets, long totalLength) throws IOException {

        long offset = 0;
        for (long nextOffset : messageOffsets) {
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append("going to read message: [");
            b.append(offset);
            b.append(", ");
            b.append(nextOffset);
            b.append(")");
            Log.writeLog(this, this.toString(), b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            messagesContainer.addMessage(is, nextOffset - offset);
            //if(!changed) { changed = true; this.contentChanged();}
            offset = nextOffset;
        }

        // last round
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("going to read last message: from offset ");
        b.append(offset);
        b.append(" to end of file - total length: ");
        b.append(totalLength);
        Log.writeLog(this, this.toString(), b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        messagesContainer.addMessage(is, totalLength - offset);
    }

    private boolean hasSufficientCrypto(ASAP_PDU_1_0 pdu) {
        if(this.getCryptoControl() == null) {
            Log.writeLog(this, this.toString(), "crypto control set to allow anything");
            return true;
        }

        boolean proceed = this.getCryptoControl().allowed2Process(pdu);
        if(!proceed) {
            Log.writeLog(this, this.toString(), "no sufficient crypto: " + pdu);
        }

        return proceed;
    }

    public void handleASAPInterest(ASAP_Interest_PDU_1_0 asapInterest, ASAP_1_0 protocol,
               String encounteredPeer, OutputStream os, EncounterConnectionType connectionType)
            throws ASAPException, IOException {

        // before we start - lets crypto: TODO can be removed - do it on communication not engine level
        if(!hasSufficientCrypto(asapInterest)) return;

        // get remote peer
        String senderID = asapInterest.getSender();

        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("handle interest from: ");
        b.append(senderID);
        b.append(" | app: ");
        b.append(asapInterest.getFormat());
        b.append(" | uri:");
        b.append(asapInterest.getChannelUri());
        Log.writeLog(this, this.toString(), b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        // init
        int workingEra = this.getOldestEra();

        // already met?
        if(this.lastSeen != null) {
            Integer lastSeenEra = this.lastSeen.get(senderID);
            if(lastSeenEra != null) workingEra = lastSeenEra;
        }

        // got even information from other side?
        Map<String, Integer> encounterMap = asapInterest.getEncounterMap();
        Log.writeLog(this, this.toString(), "received encounterMap: " + encounterMap);

        // am I in encounter list?
        if(encounterMap != null) {
            Integer eraEncounteredMe = encounterMap.get(this.owner);
            if(eraEncounteredMe != null) {
                int eraEncounter = eraEncounteredMe;
                Log.writeLog(this, this.toString(), "found me in encounter map: " + encounterMap);
                // would start with next era
                eraEncounter = ASAP.nextEra(eraEncounter);
                if (eraEncounter != workingEra && ASAP.isEraInRange(eraEncounter, this.getOldestEra(), workingEra)) {
                    // this seems to be a valid era - maybe got routed data
                    Log.writeLog(this, this.toString(),
                            "change 1st era from " + workingEra + " to " + eraEncounter);
                    workingEra = eraEncounter;
                }
            }
        }

        Log.writeLog(this, this.toString(), "transmit chunks from " + workingEra + " to era: " + this.era);

        if(workingEra == this.era) {
            // nothing todo
            b = new StringBuilder();
            b.append("there are no information before that era; ");
            b.append("we only deliver information from previous eras - nothing todo here.");
            Log.writeLog(this, this.toString(), b.toString());
        } else {
            // we iterate up to era just before current one - current one is active sync.
            int lastEra = this.getPreviousEra(this.era);

            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();  b.append("workingEra: "); b.append(workingEra);
            b.append(" | lastEra: "); b.append(lastEra); b.append(" | this.era: "); b.append(this.era);
            Log.writeLog(this, this.toString(), b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // make a breakpoint here
            if (this.memento != null) this.memento.save(this);

            this.sendChunks(this.owner, senderID, this.getChunkStorage(), protocol, workingEra, lastEra, os, true);
            Log.writeLog(this, this.toString(), "ended iterating local chunks");
        }

        /////////////////////////////////// asap routing

        if(this.routingAllowed()) {
            // iterate: what sender do we know in our side?
            for(CharSequence receivedFromID : this.getSender()) {
                if(PeerIDHelper.sameID(encounteredPeer, receivedFromID)) {
                    // do not send messages back
                    continue;
                }
                Log.writeLog(this, this.toString(), "going to route messages from " + receivedFromID);
                try {
                    ASAPStorage receivedMessagesStorage = this.getExistingIncomingStorage(receivedFromID);
                    int eraLastToSend = receivedMessagesStorage.getEra();
                    int eraFirstToSend = receivedMessagesStorage.getOldestEra();

                    // got encounter information from other peer?
                    if(encounterMap != null) {
                        Integer eraLastMet = encounterMap.get(receivedFromID);
                        if(eraLastMet != null) {
                            Log.writeLog(this, this.toString(),
                                    "found sender received encounter map; last encounter: " + eraLastMet);
                            /*
                            Peer told us last encounter era - from senders perspective of course.
                            There are several options, sketch:

                                              <------ our storage ------->
                            ............ [eraFirst] +++++++++++++++ [eraLast] .........
                               (a)                       (b)            (c)      (d)
                            a) We send everything we have
                            b) We send from b+1 until eraLast
                            c) + d) We are in sync or behind - nothing to do
                             */

                            // we would start with the next era
                            int eraAfterLastMet = ASAP.nextEra(eraLastMet);

                            if(ASAP.isEraInRange(eraAfterLastMet, eraFirstToSend, eraLastToSend)) { // case b)
                                eraFirstToSend = eraAfterLastMet;
                            }
                        }
                    }
                    this.sendChunks(receivedFromID, senderID, receivedMessagesStorage.getChunkStorage(), protocol,
                            eraFirstToSend, eraLastToSend, os,false);
                }
                catch(ASAPException e) {
                    Log.writeLogErr(this, this.toString(),
                            "internal problem: we know sender but cannot access its storage");
                }
            }
        } else {
            Log.writeLog(this, this.toString(), "engine does not send received chunks");
        }
    }

    public boolean routingAllowed() {
        return this.routingAllowed;
    }

    public void setBehaviourAllowRouting(boolean on) throws IOException {
        this.routingAllowed = on;
        this.saveStatus();
    }

    void sendInterest(CharSequence ownerID, ASAP_1_0 protocol, OutputStream os)
            throws IOException, ASAPException {

        Log.writeLog(this, this.toString(), "send interest for app/format: " + format);

        // produce encounter map
        Map<String, Integer> encounterMap = new HashMap<>();

        Set<String> encounteredPeers = this.lastSeen.keySet();
        for(String peerID : encounteredPeers) {
            try {
                int lastEra = this.getExistingIncomingStorage(peerID).getEra();
                encounterMap.put(peerID, lastEra);
            }
            catch(ASAPException e) {
                /*
                There is no storage for encountered peer. Can happen - met but has not got anything from it.
                So, take era from last seen...

                I can't follow, sorry 2021, Dec, 10th (thsc42)
                encounterMap.put(peerID, this.lastSeen.get(peerID));
                 */
            }
        }
        Log.writeLog(this, this.toString(), "send encounterMap with interest: " + encounterMap);

        protocol.interest(ownerID, null,
                format, null, ASAP_1_0.ERA_NOT_DEFINED, ASAP_1_0.ERA_NOT_DEFINED,
                os, this.getASAPCommunicationCryptoSettings().mustSign(),
                this.getASAPCommunicationCryptoSettings().mustEncrypt(),
                this.routingAllowed(),
                encounterMap);
    }

    private void sendChunks(CharSequence sender, String encounteredPeer, ASAPChunkStorage chunkStorage,
                            ASAP_1_0 protocol, int workingEra,
                            int lastEra, OutputStream os, boolean remember) throws IOException, ASAPException {
        Log.writeLog(this, this.toString(),
                "sendChunks: sender: " + sender + " | encounteredPeer: " + encounteredPeer
                        + " | workingEra: " + workingEra);

        /*
        There is a little challenge: era uses a circle of numbers
        We cannot say: higher number, later era. That rule does *not*
        apply. We can calculate next era, though.

        That loop has to be re-entered as long as working era has not
        yet reached lastEra. In other words: lastRound is reached whenever
        workingEra == lastEra. Processing lastEra is the last round
        We at at least one round!
        */

        boolean lastRound = false; // assume more than one round
        do {
            lastRound = workingEra == lastEra;

            List<ASAPInternalChunk> chunks = chunkStorage.getChunks(workingEra);
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append("start iterating chunks with working era: ");
            b.append(workingEra);
            Log.writeLog(this, this.toString(), b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            for(ASAPInternalChunk chunk : chunks) {
                boolean goAhead = true; // to avoid deep if-if-if-if structures

                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append("chunkUrl: ");
                b.append(chunk.getUri());
                b.append(" | isPublic: ");
                b.append(this.isPublic(chunk));
                b.append(" | len: ");
                b.append(chunk.getLength());
                b.append(" | recipients: ");
                b.append(chunk.getRecipients());
                Log.writeLog(this, this.toString(), b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                if(chunk.getLength() < 1) {
                    goAhead = false;
                }

                // is not a public chunk
                if (goAhead && !this.isPublic(chunk)) {
                    Set<CharSequence> recipients = chunk.getRecipients();
                    if (recipients == null || !recipients.contains(encounteredPeer)) {
                        goAhead = false;
                    }
                }

                if (goAhead) {
                    Log.writeLog(this, this.toString(), "send chunk");
                    protocol.assimilate(sender, // owner or source from received message
                            encounteredPeer, // peer to which we are connected right now
                            this.format,
                            chunk.getUri(), // channel ok
                            workingEra, // era ok
                            chunk.getLength(), // data length
                            chunk.getOffsetList(),
                            chunk.getASAPHopList(),
                            chunk.getMessageInputStream(),
                            os,
                            this.getASAPCommunicationCryptoSettings());

                    // remember sent
                    if(remember) chunk.deliveredTo(encounteredPeer);
                    Log.writeLog(this, this.toString(), "remembered delivered to " + encounteredPeer);

                    //>>>>>>>>>>>>>>>>>>>debug
                    // sent to all recipients
                    if (chunk.getRecipients().size() == chunk.getDeliveredTo().size()) {
                        Log.writeLog(this, this.toString(),
                    "#recipients == #deliveredTo chunk delivered to any potential remotePeer - could drop it");
                        if (this.isDropDeliveredChunks()) {
                            chunk.drop();
                            Log.writeLog(this, this.toString(), "chunk dropped");
                        } else {
                            Log.writeLog(this, this.toString(),
                                    "drop flag set false - engine does not remove delivered chunks");
                        }
                    }
                } else {
                    Log.writeLog(this, this.toString(), "nothing sent: empty or not on recipient list");
                }
            }

            if(remember) {
                // remember that we are in sync until that era
                this.setLastSeen(encounteredPeer, workingEra);

                // make a breakpoint here
                if (this.memento != null) this.memento.save(this);
            }

            // next era which isn't necessarily workingEra++
            workingEra = this.getNextEra(workingEra);

            // as long as not already performed last round
        } while(!lastRound);
    }

    private boolean isDropDeliveredChunks() {
        return this.dropDeliveredChunks;
    }

    public void setBehaviourDropDeliveredChunks(boolean drop) throws IOException {
        this.dropDeliveredChunks = drop;
        this.saveStatus();
    }

    @Override
    public int getNextEra(int workingEra) {
        return ASAP.nextEra(workingEra);
    }

    @Override
    public int getPreviousEra(int workingEra) {
        return ASAP.previousEra(workingEra);
    }

    private int getEraStartSync(String peer) {
        Integer lastEra = this.lastSeen.get(peer);
        if(lastEra == null) {
            return this.getOldestEra();
        }
        
        return lastEra;
    }

    private void setLastSeen(String peer, int workingEra) {
        this.lastSeen.put(peer, era);
    }

    @Override
    public int getOldestEra() {
        return this.oldestEra;
    }
    
    @Override
    public int getEra() {
        return this.era;
    }

    @Override
    public CharSequence getFormat()  {
        return this.format;
    }

    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                            Online management                                           //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void activateOnlineMessages(ASAPInternalPeer multiEngine) {
        if(this.asapOnlineMessageSender == null) {
            Log.writeLog(this, this.toString(),
                    "(" + this.format + ") created new online message sender");
            this.attachASAPMessageAddListener(new ASAPOnlineMessageSenderEngineSide(multiEngine));
        } else {
            Log.writeLog(this, this.toString(),
                    "(" + this.format + ") online message sender already running");
        }
    }

    public void deactivateOnlineMessages() {
        this.detachASAPMessageAddListener();
    }
}
