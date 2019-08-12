package net.sharksystem.asap;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 *
 * @author thsc
 */
class ASAPChunkFS implements ASAPChunk {
    public static final String META_DATA_EXTENSION = "meta";
    public static final String DATA_EXTENSION = "content";
    public static final String DEFAULT_URL = "content://sharksystem.net/noContext";
    private final ASAPChunkStorageFS storage;
    private String uri = DEFAULT_URL;
    private List<CharSequence> recipients;
    private List<Long> messageStartOffsets = new ArrayList<>();
    private File metaFile;
    private File messageFile;
    
    private int era;
    private String sender;
    
    
    ASAPChunkFS(ASAPChunkStorageFS storage, String targetUri, int era) throws IOException {
        this(storage, targetUri, era, null);
    }

    ASAPChunkFS(ASAPChunkStorageFS storage, String targetUri, int era, String sender) throws IOException {
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

    ASAPChunkFS(ASAPChunkStorageFS storage, String trunkName) throws IOException {
        this.storage = storage;
        this.uri = ASAPChunkFS.DEFAULT_URL;
        
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
            this.recipients = new ArrayList<>();
            this.messageStartOffsets = new ArrayList<>();
        }
    }
    
    private void saveStatus() throws IOException {
        this.writeMetaData(this.metaFile);
    }
    
    @Override
    public List<CharSequence> getRecipients() {
        return this.recipients;
    }

    @Override
    public void addRecipient(CharSequence recipient) throws IOException {
        this.recipients.add(recipient);
        
        this.writeMetaData(this.metaFile);
    }

    @Override
    public void setRecipients(List<CharSequence> newRecipients) throws IOException {
        this.recipients = new ArrayList<>();
        for(CharSequence recipient : newRecipients) {
            this.recipients.add(recipient);
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

    @Override
    public void addMessage(CharSequence message) throws IOException {
        this.addMessage(message.toString().getBytes());
    }

    @Override
    public void addMessage(byte[] messageAsBytes) throws IOException {
        if(messageAsBytes.length > Integer.MAX_VALUE) {
            throw new IOException("message must not be longer than Integer.MAXVALUE");
        }

        long offset = this.messageFile.length();

        OutputStream os = new FileOutputStream(this.messageFile, true);

        os.write(messageAsBytes);

        os.close();

        // remember offset if not 0
        if(offset > 0) {
            this.messageStartOffsets.add(offset);
            this.saveStatus();
        }
    }

    @Override
    public Iterator<byte[]> getMessagesAsBytes() throws IOException {
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

    @Override
    public Iterator<CharSequence> getMessages() throws IOException {
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

    private static final String RECIPIENTS_LIST_DELIMITER = "|||";

    private boolean readMetaData(File metaFile) throws IOException {
        if(!metaFile.exists()) return false;
        // read data from metafile
        DataInputStream dis = new DataInputStream(new FileInputStream(metaFile));

        try {
            this.uri = dis.readUTF();
        }
        catch(EOFException eof) {
            // file empty
            return false;
        }
        
        this.recipients = new ArrayList<CharSequence>();
        
        try {
            String recipientsList = dis.readUTF();
            
            StringTokenizer t = new StringTokenizer(recipientsList, 
                    RECIPIENTS_LIST_DELIMITER);
            
            while(t.hasMoreTokens()) {
                this.recipients.add(t.nextToken());
            }
            // finally read offset list
            String offsetList = dis.readUTF();
            this.messageStartOffsets = this.messageOffsetString2List(offsetList);
        }
        catch(IOException ioe) {
            // no more data - ok
        }
        finally {
            dis.close();
        }
        
        return true;
    }
    
    private void writeMetaData(File metaFile) throws IOException {
        // write data to metafile
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(metaFile));
        
        dos.writeUTF(this.uri);
        StringBuilder b = new StringBuilder();
        
        boolean first = true;
        if(this.recipients != null && !this.recipients.isEmpty()) {
            for(CharSequence recipient : this.recipients) {
                if(recipient != null) {
                    if(!first) {
                        b.append(RECIPIENTS_LIST_DELIMITER);
                    } else {
                        first = false;
                    }
                    b.append(recipient);
                }
            }
        }
        dos.writeUTF(b.toString());

        // write offsetList
        dos.writeUTF(this.messageStartOffsetListAsString());
        
        dos.close();
    }

    private String messageStartOffsetListAsString() {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for(Long offset : this.messageStartOffsets) {
            if(!first) {
                sb.append(RECIPIENTS_LIST_DELIMITER);
            }

            sb.append(offset.toString());
        }

        return sb.toString();
    }

    private ArrayList<Long> messageOffsetString2List(String s) {
        ArrayList<Long> longList = new ArrayList<>();

        if(s == null || s.length() == 0) return longList;

        StringTokenizer t = new StringTokenizer(s, RECIPIENTS_LIST_DELIMITER);

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

    private class MessageIter implements Iterator {
        private final List<byte[]> byteMessages;
        private int nextIndex;
        private String nextString;
        
        
        MessageIter(List<byte[]> byteMessages) throws FileNotFoundException {
            this.byteMessages = byteMessages;
            this.nextIndex = 0;
        }
        
        @Override
        public boolean hasNext() {
            return this.byteMessages.size() > nextIndex;
        }

        @Override
        public String next() {
            if(!this.hasNext()) {
                throw new NoSuchElementException("no more messages");
            }

            return new String(this.byteMessages.get(nextIndex++));
        }
    }
}
