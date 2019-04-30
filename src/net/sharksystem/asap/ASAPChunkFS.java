package net.sharksystem.asap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private ArrayList<CharSequence> recipients;
    private File metaFile;
    private File messageFile;
    
    private int numberMessage = 0;
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
        
        if(!this.readMetaData(this.metaFile)) {
            // no metadate to be read - set
            this.writeMetaData(this.metaFile);
            this.recipients = new ArrayList<>();
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
    public void add(CharSequence message) throws IOException {
        DataOutputStream dos;
        dos = new DataOutputStream(new FileOutputStream(this.messageFile, true));
        
        dos.writeUTF((String) message);
        this.numberMessage++;
        // keep message counter
        this.saveStatus();
    }

    @Override
    public Iterator<CharSequence> getMessages() throws IOException {
        try {
            return new MessageIter(this.messageFile);
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
            this.numberMessage = dis.readInt();
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
        // read data from metafile
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(metaFile));
        
        dos.writeUTF(this.uri);
        dos.writeInt(this.numberMessage);
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
        
        dos.close();
    }

    @Override
    public int getNumberMessage() {
        return this.numberMessage;
    }

    @Override
    public int getEra() throws IOException {
        return this.era;
    }

    private class MessageIter implements Iterator {

        private final DataInputStream dis;
        private String nextString;
        
        
        MessageIter(File messageFile) throws FileNotFoundException {
            this.dis = new DataInputStream(new FileInputStream(messageFile));
            
            this.lookahead();
        }
        
        private void lookahead() {
            try {
                this.nextString = this.dis.readUTF();
            } catch (IOException ex) {
                // empty
                this.nextString = null;
            }
        }

        @Override
        public boolean hasNext() {
            return this.nextString != null;
        }

        @Override
        public String next() {
            if(this.nextString != null) {
                String s = this.nextString;
                this.lookahead();
                return s;
            }
            
            throw new NoSuchElementException("no more messages");
        }
    }
}
