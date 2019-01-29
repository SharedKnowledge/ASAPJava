package net.sharksystem.aasp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * AASPEngine that stores data in file system.
 * @author thsc
 */
public class AASPEngineFS extends AASPEngine {
    public static final String MEMENTO_FILENAME = "aaspCurrentAttributes";
    private final String rootDirectory;
    private AASPChunkStorageFS chunkStorageFS;
    
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
                && !owner.equalsIgnoreCase(engine.owner)) {
            
                    throw new AASPException("cannot overwrite folder of a "
                        + "a non-anonymous different owner: " + engine.owner);
        } 
        
        // replacing owner could be done
        if(owner != null) {
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
    public AASPChunkStorage getReceivedChunkStorage(CharSequence sender) {
        String dir = this.rootDirectory + "/" + sender;
        return new AASPChunkStorageFS(dir);
    }

}
