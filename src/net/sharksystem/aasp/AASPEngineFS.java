package net.sharksystem.aasp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * AASPEngine that stores data in file system.
 * @author thsc
 */
public class AASPEngineFS extends AASPEngine {
    public static final String MEMENTO_FILENAME = "aaspCurrentAttributes";
    private final String rootDirectory;
    
    public static final String DEFAULT_ROOT_FOLDER_NAME = "SHARKSYSTEM_AASP";
    
    private AASPEngineFS(String rootDirectory,
            AASPChunkStorageFS chunkStorage) 
        throws AASPException, IOException {
        
        super(new AASPChunkStorageFS(rootDirectory));
        
        this.rootDirectory = rootDirectory;
    }
    
    public static AASPStorage getAASPChunkStorage(String owner, String rootDirectory) 
            throws IOException, AASPException {
        
        // check if root directory already exists. If not srt it up
        File root = new File(rootDirectory);
        if(!root.exists()) {
            root.mkdirs();
        }
        
        return AASPEngineFS.getAASPEngine(owner, rootDirectory);
        
    }
    
    public static AASPStorage getAASPChunkStorage(String rootDirectory) 
            throws IOException, AASPException {
        
        return AASPEngineFS.getAASPChunkStorage(AASPEngineFS.DEFAULT_OWNER, rootDirectory);
        
    }
    
    public static AASPEngine getAASPEngine(String owner, String rootDirectory) 
            throws IOException, AASPException {
        
        // root directory must exist when setting up an engine
        File root = new File(rootDirectory);
        if(!root.exists() || !root.isDirectory()) {
            throw new AASPException("chunk root directory must exist when creating an AASPEngine");
        }
        
        AASPEngineFS engine = new AASPEngineFS(
                rootDirectory, 
                new AASPChunkStorageFS(rootDirectory));

        
        AASPMementoFS mementoFS = engine.getMemento(rootDirectory);
        engine.memento = mementoFS;
        
        mementoFS.restore(engine);
        
        // reset owner?
        if(owner != null && engine.owner != null
                && !engine.owner.equalsIgnoreCase(AASPEngine.ANONYMOUS_OWNER)
                && !engine.owner.equalsIgnoreCase(AASPEngine.DEFAULT_OWNER)
                && !owner.equalsIgnoreCase(AASPEngine.ANONYMOUS_OWNER)
                && !owner.equalsIgnoreCase(AASPEngine.DEFAULT_OWNER)
                && !owner.equalsIgnoreCase(engine.owner)) {
            
                    throw new AASPException("cannot overwrite folder of a "
                        + "a non-anonymous different owner: " + engine.owner);
        } 
        
        // replacing owner could be done
        if(owner != null
                && !owner.equalsIgnoreCase(AASPEngine.ANONYMOUS_OWNER)
                && !owner.equalsIgnoreCase(AASPEngine.DEFAULT_OWNER)
        ) {
            engine.owner = owner;
            mementoFS.save(engine);
        }
        
        return engine;
    }

    public static AASPEngine getAASPEngine(String rootDirectory) 
            throws IOException, AASPException {
            
        return AASPEngineFS.getAASPEngine(null, rootDirectory);

    }
    
    private AASPMementoFS getMemento(String rootDirectory) {
        return new AASPMementoFS(new File(rootDirectory));
    }
    
    private HashMap<CharSequence, AASPChunkStorage> storageList = new HashMap<>();

    @Override
    public AASPChunkStorage getIncomingChunkStorage(CharSequence sender) {
        String dir = this.rootDirectory + "/" + sender;
        return new AASPChunkStorageFS(dir);
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
                    AASPEngineFS.removeFolder(fileInDir.getAbsolutePath());
                } else {
                    try {
                        fileInDir.delete();
                    } catch (RuntimeException e) {
                        System.err.println("AASPEngineFS: cannot file:" + e.getLocalizedMessage());
                        // try next
                    }
                }
            }
        }

        dir.delete();
    }
}
