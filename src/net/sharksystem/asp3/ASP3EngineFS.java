package net.sharksystem.asp3;

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

        
        ASP3EngineFS.ASP3MementoFS mementoFS = engine.getMemento(rootDirectory);
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
    
    private ASP3EngineFS.ASP3MementoFS getMemento(String rootDirectory) {
        return new ASP3MementoFS(new File(rootDirectory));
    }
    
    private HashMap<CharSequence, ASP3Storage> storageList = new HashMap<>();

    @Override
    public ASP3Storage getReceivedChunkStorage(CharSequence sender) {
        String dir = this.rootDirectory + "/" + sender;
        return new ASP3StorageFS(dir);
    }

    class ASP3MementoFS implements ASP3Memento {
        private final File rootDirectory;

        public ASP3MementoFS(File rootDirectory) {
            this.rootDirectory = rootDirectory;
        }
        
        

        @Override
        public void save(ASP3Engine engine) throws IOException {
            String fName = this.getMementoFileName();
            
            File file = new File(fName);
            if(!file.exists()) {
                if(!file.createNewFile()) {
                    throw new IOException("could not create file (problems with directory?): " + fName);
                }
            }
            
            DataOutputStream dos = new DataOutputStream(
                                    new FileOutputStream(fName));
            
            dos.writeUTF(engine.owner);
            dos.writeInt(engine.era);
            dos.writeInt(engine.oldestEra);
            
            // write lastSeen hash map
            if(engine.lastSeen != null && !engine.lastSeen.isEmpty()) {
                for(String key : engine.lastSeen.keySet()) {
                    Integer era = engine.lastSeen.get(key);
                    
                    // write peer and era
                    dos.writeUTF(key);
                    dos.writeInt(era);
                }
            }
        }
        
        private void setDefaults(ASP3Engine engine) {
            // set defaults
            engine.owner = ASP3Engine.DEFAULT_OWNER;
            engine.era = ASP3Engine.DEFAULT_INIT_ERA;
            engine.oldestEra = ASP3Engine.DEFAULT_INIT_ERA;
            engine.lastSeen = new HashMap<>();
        }
        
        public void restore(ASP3Engine engine) throws IOException {
            String fName = this.getMementoFileName();
            
            File file = new File(fName);
            if(!file.exists()) {
                this.setDefaults(engine);
                return;
            }
            
            DataInputStream dis = new DataInputStream(
                                    new FileInputStream(file));
            
            engine.owner = dis.readUTF();
            engine.era = dis.readInt();
            engine.oldestEra = dis.readInt();
            
            // try to read lastSeen list
            boolean first = true;
            try {
                for(;;) { // escapes from that loop via ioexception
                    String peer = dis.readUTF();
                    // got one
                    if(first) {
                        // init empty list
                        engine.lastSeen = new HashMap<>();
                        first = false;
                    }
                    
                    Integer era = dis.readInt();
                    
                    // remember
                    engine.lastSeen.put(peer, era);
                }
            }
            catch(IOException ioe) {
                    // ok  no more data
            }
        }
        
        private String getMementoFileName() {
            return this.rootDirectory + "/" + ASP3EngineFS.MEMENTO_FILENAME;
        }
    }
}
