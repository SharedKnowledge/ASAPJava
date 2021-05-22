package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.asap.utils.Helper;

import java.io.*;
import java.util.*;

/**
 *
 * @author thsc
 */
public class ASAPInternalChunkFS implements ASAPInternalChunk {
    public static final String META_DATA_EXTENSION = "meta";
    public static final String DATA_EXTENSION = "content";
    public static final String DEFAULT_URL = "content://sharksystem.net/noContext";
    private final ASAPChunkStorageFS storage;
    private String sender;
    private String uri = DEFAULT_URL;
    private Set<CharSequence> recipients;
    private List<CharSequence> deliveredTo;
    private List<Long> messageStartOffsets = new ArrayList<>();
    private File metaFile;
    private File messageFile;

    private List<ASAPHop> hopList;
    
    private int era;

    private HashMap<String, String> extraData = new HashMap<>();


    @Override
    public void clone(ASAPInternalChunk chunkSource) throws IOException {
        this.uri = chunkSource.getUri();
        this.recipients = chunkSource.getRecipients();
        this.extraData = chunkSource.getExtraData();

        this.saveStatus();
    }

    @Override
    public void copyMetaData(ASAPChannel channel) throws IOException {
        this.uri = channel.getUri().toString();
        this.recipients = channel.getRecipients();
        this.extraData = channel.getExtraData();

        this.saveStatus();
    }

    @Override
    public List<ASAPHop> getASAPHopList() {
        return this.hopList;
    }

    public void setASAPHopList(List<ASAPHop> asapHopList) throws IOException {
        this.hopList = asapHopList;
        this.saveStatus();
    }

    public HashMap<String, String> getExtraData() {
        return this.extraData;
    }

    @Override
    public void deliveredTo(String peer) throws IOException {
        this.deliveredTo.add(peer);
        this.saveStatus();
    }

    @Override
    public List<CharSequence> getDeliveredTo() {
        return this.deliveredTo;
    }

    ASAPInternalChunkFS(ASAPChunkStorageFS storage, String uri, int era) throws IOException {
        this(storage, uri, era, null);
    }

    ASAPInternalChunkFS(ASAPChunkStorageFS storage, String targetUri, int era, String sender) throws IOException {
        this.storage = storage;
        if(targetUri != null) {
            this.uri = targetUri;
        }
        this.era = era;
        this.sender = sender;
        
        String trunkName = this.storage.setupChunkFolder(era, targetUri);
        
        // init
        this.initFiles(trunkName);
    }

    public ASAPInternalChunkFS(ASAPChunkStorageFS storage, String trunkName) throws IOException {
        this.storage = storage;
        this.uri = ASAPInternalChunkFS.DEFAULT_URL;
        
        this.initFiles(trunkName);
    }

    private void initFiles(String trunkName) throws IOException {
        String messageFileName = trunkName + "." +  DATA_EXTENSION;
        String metaFileName = trunkName + "." + META_DATA_EXTENSION;
        
        this.messageFile = new File(messageFileName);
        this.metaFile = new File(metaFileName);
        
        // init meta file - message file keeps untouched (good idea?)
        if(!this.metaFile.exists()) {
            this.metaFile.createNewFile();
        }

        // try to read existing meta data
        if(!this.readMetaData(this.metaFile)) {
            // no metadate to be read - set defaults
            this.writeMetaData(this.metaFile);
            this.recipients = new HashSet<>();
            this.deliveredTo = new ArrayList<>();
            this.messageStartOffsets = new ArrayList<>();
            this.hopList = new ArrayList<>();
        }
    }
    
    private void saveStatus() throws IOException {
        this.writeMetaData(this.metaFile);
    }
    
    @Override
    public Set<CharSequence> getRecipients() {
        return this.recipients;
    }

    @Override
    public void addRecipient(CharSequence recipient) throws IOException {
        this.recipients.add(recipient);
        this.writeMetaData(this.metaFile);
    }

    @Override
    public void setRecipients(Collection<CharSequence> newRecipients) throws IOException {
        this.recipients = new HashSet<>();
        if(recipients != null) {
            for (CharSequence recipient : newRecipients) {
                this.recipients.add(recipient);
            }
        }
        
        this.writeMetaData(this.metaFile);
    }

    @Override
    public void removeRecipient(CharSequence recipient) throws IOException {
        this.recipients.remove(recipient);
        this.writeMetaData(this.metaFile);
    }

    @Override
    public String getUri() {
        return (String) this.uri;
    }

    /*
    @Override
    public void addMessage(CharSequence message) throws IOException {
        this.addMessage(message.toString().getBytes());
    }
*/
    @Override
    public void addMessage(byte[] messageAsBytes) throws IOException {
        if(messageAsBytes.length > Integer.MAX_VALUE) {
            throw new IOException("message must not be longer than Integer.MAXVALUE");
        }

        InputStream is = new ByteArrayInputStream(messageAsBytes);

        this.addMessage(is, messageAsBytes.length);
    }

    public void addMessage(InputStream messageByteIS, long length) throws IOException {
        if(length > Integer.MAX_VALUE) {
            throw new IOException("message must not be longer than Integer.MAXVALUE");
        }

        long offset = this.messageFile.length();

//        Log.writeLog(this, "chunk file offset == " + offset);
        OutputStream os = new FileOutputStream(this.messageFile, true);

//        Log.writeLog(this, "write message to the end of chunk file");
        while(length-- > 0) {
            os.write(messageByteIS.read());
        }

//        Log.writeLog(this, "closing");
        os.close();

        // remember offset if not 0
        if(offset > 0) {
            this.messageStartOffsets.add(offset);
            this.saveStatus();
        }
    }

    @Override
    public Iterator<byte[]> getMessages() throws IOException {
        return this.getMessagesAsBytesList().iterator();
    }

    private List<byte[]> getMessagesAsBytesList() throws IOException {
        List<byte[]> byteMessageList = new ArrayList<>();

        if(this.messageFile.length() > 0) {
            InputStream is = new FileInputStream((this.messageFile));
            long offset = 0;
            for(Long nextOffset : this.messageStartOffsets) {
                long messageLenLong = nextOffset.longValue() - offset;
                if(messageLenLong > Integer.MAX_VALUE) {
                    throw new IOException("message longer than Integer.MAXVALUE");
                }

                int messageLen = (int) messageLenLong;
                byte[] messageBytes = new byte[messageLen];

                is.read(messageBytes);

                byteMessageList.add(messageBytes);

                offset = nextOffset;
            }

            // read last one
            long messageLenLong = this.messageFile.length() - offset;
            if(messageLenLong > Integer.MAX_VALUE) {
                throw new IOException("message longer than Integer.MAXVALUE");
            }

            int messageLen = (int) messageLenLong;
            byte[] messageBytes = new byte[messageLen];
            is.read(messageBytes);
            byteMessageList.add(messageBytes);
        }

        return byteMessageList;
    }

    public long getLength() {
        return this.messageFile.length();
    }

    @Override
    public List<Long> getOffsetList() {
        return this.messageStartOffsets;
    }

    @Override
    public InputStream getMessageInputStream() {
        InputStream is = null;
        try {
            is = new FileInputStream(this.messageFile);
        } catch (FileNotFoundException e) {
            // cannot happen - is checked before
        }

        return is;
    }

    @Override
    public void putExtra(String key, String value) throws IOException {
        if(key == null || value == null) {
            throw new IOException("null values are not allowed in extra data");
        }
        this.extraData.put(key, value);
        this.saveStatus();
    }

    @Override
    public CharSequence removeExtra(String key) throws IOException {
        if(key == null) throw new IOException("null key not allowed");
        String removed = this.extraData.remove(key);
        this.saveStatus();
        return removed;
    }

    @Override
    public CharSequence getExtra(String key) throws IOException {
        if(key == null) throw new IOException("null key not allowed");
        return this.extraData.get(key);
        // no status change
    }

    @Override
    public Iterator<CharSequence> getMessagesAsCharSequence() throws IOException {
        try {
            return new MessageIter(this.getMessagesAsBytesList());
        } catch (FileNotFoundException ex) {
            throw new IOException(ex.getLocalizedMessage());
        }
    }

    @Override
    public void drop() {
        this.metaFile.delete();
        this.messageFile.delete();
    }

    private boolean readMetaData(File metaFile) throws IOException {
        if(!metaFile.exists()) return false;
        // read data from metafile
        DataInputStream dis = new DataInputStream(new FileInputStream(metaFile));

        try {
            // do it as first element - shure how many bytes we read..
            this.hopList = ASAPSerialization.readASAPHopList(dis);

            this.uri = dis.readUTF();
            this.setExtraByString(dis.readUTF());
        }
        catch(EOFException | ASAPException eof) {
            // file empty
            return false;
        }
        
        try {
            this.recipients = Helper.string2CharSequenceSet(dis.readUTF());
            this.deliveredTo = Helper.string2CharSequenceList(dis.readUTF());

            // finally read offset list
            String offsetList = dis.readUTF();
            this.messageStartOffsets = this.messageOffsetString2List(offsetList);
        }
        catch(IOException e) {
            // no more data - ok
        } finally {
            dis.close();
        }
        
        return true;
    }

    private void writeMetaData(File metaFile) throws IOException {
        // write data to metafile
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(metaFile));

        // do it as first element - shure how many bytes we read..
        ASAPSerialization.writeASAPHopList(this.hopList, dos);
        
        dos.writeUTF(this.uri);
        dos.writeUTF(this.getExtraAsString());
        dos.writeUTF(Helper.collection2String(this.recipients));
        dos.writeUTF(Helper.collection2String(this.deliveredTo));

        // write offsetList
        dos.writeUTF(this.messageStartOffsetListAsString());
        
        dos.close();
    }

    private String messageStartOffsetListAsString() {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for(Long offset : this.messageStartOffsets) {
            if(!first) {
                sb.append(Helper.SERIALIZATION_DELIMITER);
            }
            first = false;
            sb.append(offset.toString());
        }

        return sb.toString();
    }

    private String getExtraAsString() throws IOException {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for(String key : this.extraData.keySet()) {
            String value = this.extraData.get(key);
            if(value == null) {
                throw new IOException("null value not allowed in extra data");
            };

            if(first) { first = false; }
            else { sb.append(Helper.SERIALIZATION_DELIMITER); }

            sb.append(key);
            sb.append(Helper.SERIALIZATION_DELIMITER);
            sb.append(value);
        }

        return sb.toString();
    }

    private void setExtraByString(String extraString) throws IOException {
        if(extraString == null) return;

        try {
            HashMap<String, String> extra = new HashMap<>();
            StringTokenizer st = new StringTokenizer(extraString, Helper.SERIALIZATION_DELIMITER);
            while (st.hasMoreTokens()) {
                String key = st.nextToken();
                String value = st.nextToken();

                extra.put(key, value);
            }

            this.extraData = extra;
        }
        catch(RuntimeException e) {
            // missing token or something
            throw new IOException(e.getLocalizedMessage());
        }
    }

    private ArrayList<Long> messageOffsetString2List(String s) {
        ArrayList<Long> longList = new ArrayList<>();

        if(s == null || s.length() == 0) return longList;

        StringTokenizer t = new StringTokenizer(s, Helper.SERIALIZATION_DELIMITER);

        while(t.hasMoreTokens()) {
            Long offsetLong = Long.parseLong(t.nextToken());
            longList.add(offsetLong);
        }

        return longList;
    }

    @Override
    public int getNumberMessage() {
        if(this.messageFile.length() == 0) return 0;

        return this.messageStartOffsets.size() + 1;
    }

    @Override
    public int getEra() throws IOException {
        return this.era;
    }

}
