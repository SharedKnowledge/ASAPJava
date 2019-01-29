package net.sharksystem.aasp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * ASP3Engine that stores data in file system.
 * @author thsc
 */
public class ASP3EngineFS extends ASP3Engine {
    public static final String MEMENTO_FILENAME = "asp3CurrentAttributes";
    private final String rootDirectory;
    private ASP3StorageFS chunkStorageFS;
    
    private ASP3EngineFS(String rootDirectory,
            ASP3StorageFS chunkStorage) 
        throws ASP3Exception, IOException {
        
        super(new ASP3StorageFS(rootDirectory));
        
        this.rootDirectory = rootDirectory;
    }
    
    public static ASP3ChunkStorage getASP3ChunkStorage(String owner, String rootDirectory) 
            throws IOException, ASP3Exception {
        
        // check if root directory already exists. If not srt it up
        File root = new File(rootDirectory);
        if(!root.exists()) {
            root.mkdirs();
        }
        
        return ASP3EngineFS.getASP3Engine(owner, rootDirectory);
        
    }
    
    public static ASP3ChunkStorage getASP3ChunkStorage(String rootDirectory) 
            throws IOException, ASP3Exception {
        
        return ASP3EngineFS.getASP3ChunkStorage(ASP3EngineFS.DEFAULT_OWNER, rootDirectory);
        
    }
    
    public static ASP3Engine getASP3Engine(String owner, String rootDirectory) 
            throws IOException, ASP3Exception {
        
        // root directory must exist when setting up an engine
        File root = new File(rootDirectory);
        if(!root.exists() || !root.isDirectory()) {
            throw new ASP3Exception("chunk root directory must exist when creating an ASP3Engine");
        }
        
        ASP3EngineFS engine = new ASP3EngineFS(
                rootDirectory, 
                new ASP3StorageFS(rootDirectory));

        
        ASP3MementoFS mementoFS = engine.getMemento(rootDirectory);
        engine.memento = mementoFS;
        
        mementoFS.restore(engine);
        
        // reset owner?
        if(owner != null && engine.owner != null
                && !engine.owner.equalsIgnoreCase(ASP3Engine.ANONYMOUS_OWNER) 
                && !owner.equalsIgnoreCase(engine.owner)) {
            
                    throw new ASP3Exception("cannot overwrite folder of a "
                        + "a non-anonymous different owner: " + engine.owner);
        } 
        
        // replacing owner could be done
        if(owner != null) {
            engine.owner = owner;
            mementoFS.save(engine);
        }
        
        return engine;
    }

    public static ASP3Engine getASP3Engine(String rootDirectory) 
            throws IOException, ASP3Exception {
            
        return ASP3EngineFS.getASP3Engine(null, rootDirectory);

    }
    
    private ASP3MementoFS getMemento(String rootDirectory) {
        return new ASP3MementoFS(new File(rootDirectory));
    }
    
    private HashMap<CharSequence, ASP3Storage> storageList = new HashMap<>();

    @Override
    public ASP3Storage getReceivedChunkStorage(CharSequence sender) {
        String dir = this.rootDirectory + "/" + sender;
        return new ASP3StorageFS(dir);
    }

}
