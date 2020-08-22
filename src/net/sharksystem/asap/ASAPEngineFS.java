package net.sharksystem.asap;

import net.sharksystem.Utils;
import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ASAPEngine that stores data in file system.
 * @author thsc
 */
public class ASAPEngineFS extends ASAPEngine {
    public static final String MEMENTO_FILENAME = "asapCurrentAttributes";
    private final String rootDirectory;
    
    public static final String DEFAULT_ROOT_FOLDER_NAME = "SHARKSYSTEM_ASAP";

    private ASAPEngineFS(String owner,
                         String rootDirectory, ASAPChunkStorageFS chunkStorage, CharSequence format)
        throws ASAPException, IOException {
        
        super(new ASAPChunkStorageFS(rootDirectory, format.toString()), format);

        this.owner = owner;
        this.rootDirectory = rootDirectory;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("owner: ");
        sb.append(this.getClass().getSimpleName());
        sb.append(" | ");
        sb.append(this.getOwner());
        sb.append(" | oldestEra: ");
        sb.append(this.getOldestEra());
        sb.append(" | era: ");
        sb.append(this.getEra());
        sb.append(" | rootFolder: ");
        sb.append(this.rootDirectory);

        return sb.toString();
    }

    public String getRootFolder() {
        return this.rootDirectory;
    }

    public String getOwner() {
        return this.owner;
    }

    public static ASAPEngine getASAPStorage(String owner, String rootDirectory, CharSequence format)
            throws IOException, ASAPException {
        
        return ASAPEngineFS.getASAPEngine(owner, rootDirectory, format);
    }

    public static ASAPEngine getExistingASAPEngineFS(String rootDirectory)
            throws IOException, ASAPException {

        // the same
        return ASAPEngineFS.getASAPEngineFS(null, rootDirectory, null);
    }

    /**
     * get / create engine
     * @param owner
     * @param rootDirectory
     * @param format
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    public static ASAPEngine getASAPEngine(String owner, String rootDirectory, CharSequence format)
            throws IOException, ASAPException {

        // check if root directory already exists. If not set it up
        File root = new File(rootDirectory);
        if(!root.exists()) {
            root.mkdirs();
        }

        return ASAPEngineFS.getASAPEngineFS(owner, rootDirectory, format);
    }

    public ASAPStorage refresh() throws IOException, ASAPException {
        this.memento.save(this);
        return ASAPEngineFS.getExistingASAPEngineFS(this.rootDirectory);
    }

    /**
     *
     * @param owner can be null - restored
     * @param rootDirectory must not be null
     * @param format can be null - restored
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    static ASAPEngineFS getASAPEngineFS(String owner, String rootDirectory, CharSequence format)
            throws IOException, ASAPException {
        
        // root directory must exist when setting up an engine
        File root = new File(rootDirectory);
        if(!root.exists() || !root.isDirectory()) {
            throw new ASAPException("chunk root directory must exist when creating an ASAPEngine: " + rootDirectory);
        }

        String formatString = format != null ? format.toString() : ASAP_1_0.ANY_FORMAT;
        ASAPEngineFS engine = new ASAPEngineFS(
                owner,
                rootDirectory, 
                new ASAPChunkStorageFS(rootDirectory, formatString),
                ASAP_1_0.ANY_FORMAT // set to default - real value is restored by memento anyway
        );

        
        ASAPMementoFS mementoFS = engine.getMemento(rootDirectory);
        engine.memento = mementoFS;
        
        mementoFS.restore(engine);

        if(format != null) {
            // overwrite default - actually set format
            if(engine.format.equalsIgnoreCase(ASAP_1_0.ANY_FORMAT.toString())) {
                engine.format = format.toString();
            }
            else { // cannot overwrite a non-default format
                if(!format.toString().equalsIgnoreCase(engine.format)) {
                    throw new ASAPException("cannot overwrite existing format (" + format + "with another one: ("
                            + engine.format + ")");
                }
            }
        }

        // reset owner?
        if(owner != null && engine.owner != null
                && !engine.owner.equalsIgnoreCase(ANONYMOUS_OWNER)
                && !engine.owner.equalsIgnoreCase(DEFAULT_OWNER)
                && !owner.equalsIgnoreCase(ANONYMOUS_OWNER)
                && !owner.equalsIgnoreCase(DEFAULT_OWNER)
                && !owner.equalsIgnoreCase(engine.owner)) {
            
                    throw new ASAPException("cannot overwrite folder of a "
                        + "non-anonymous but different owner: " + engine.owner);
        } 
        
        // replacing owner could be done
        if(owner != null
                && !owner.equalsIgnoreCase(ANONYMOUS_OWNER)
                && !owner.equalsIgnoreCase(DEFAULT_OWNER)
        ) {
            engine.owner = owner;
        }

        // save changes
        mementoFS.save(engine);

        return engine;
    }
    
    private void restoreFromMemento() throws IOException {
        ASAPMementoFS mementoFS = this.getMemento(rootDirectory);
        this.memento = mementoFS;

        mementoFS.restore(this);

    }

    @Override
    public void add(CharSequence uri, byte[] messageAsBytes) throws IOException {
        // always re-read metainformation
        this.restoreFromMemento();
        
        // do the real work
        super.add(uri, messageAsBytes);
    }

    /*
    public static ASAPEngine getASAPEngine(String rootDirectory, CharSequence format)
            throws IOException, ASAPException {
            
        return ASAPEngineFS.getASAPEngine(null, rootDirectory, format);

    }*/
    
    private ASAPMementoFS getMemento(String rootDirectory) {
        return new ASAPMementoFS(new File(rootDirectory));
    }
    
    private HashMap<CharSequence, ASAPChunkStorage> storageList = new HashMap<>();

    @Override
    public ASAPChunkStorage getReceivedChunksStorage(CharSequence sender) {
        String dir = this.rootDirectory + "/" + sender;
        return new ASAPChunkStorageFS(dir, this.format, this.era);
    }

    public ASAPStorage getExistingIncomingStorage(CharSequence sender) throws IOException, ASAPException {
        String dir = this.rootDirectory + "/" + sender;
        return ASAPEngineFS.getExistingASAPEngineFS(dir);
    }

    @Override
    public List<CharSequence> getSender() {
        List<CharSequence> senderList = new ArrayList<>();

        File dir = new File(this.rootDirectory);

        String[] dirEntries = dir.list();

        if (dirEntries != null) {
            for (String fileName : dirEntries) {
                // era folder?
                try {
                    Integer.parseInt(fileName);
                    // a number. It is a era folder go ahead
                    continue;
                } catch (NumberFormatException e) {
                    // no number - that's ok!
                }

                File fileInDir = new File(this.rootDirectory + "/" + fileName);
                if (fileInDir.isDirectory()) {
                    senderList.add(fileName);
                }
            }
        }

        return senderList;
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //                                         helper                                     //
    ////////////////////////////////////////////////////////////////////////////////////////

    public static void removeFolder(String eraPathName) {
        File dir = new File(eraPathName);

        String[] dirEntries = dir.list();

        if(dirEntries != null) {
            for(String fileName : dirEntries) {
                File fileInDir = new File(eraPathName + "/" + fileName);
                if(fileInDir.isDirectory()) {
                    ASAPEngineFS.removeFolder(fileInDir.getAbsolutePath());
                } else {
                    try {
                        if(!fileInDir.delete()) {
                            System.out.println("ASAPEngineFS: cannot delete file (try deleteOnExit):" + fileInDir);
                        }
                    } catch (RuntimeException e) {
                        System.err.println("ASAPEngineFS: cannot file:" + e.getLocalizedMessage());
                        // try next
                    }
                }
            }
        }

        dir.delete();
        dir.deleteOnExit();
        try {
            Thread.sleep(1); // give file system a moment
        } catch (InterruptedException e) {
            // nobody wants to know
        }
    }
}
