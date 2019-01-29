package net.sharksystem.aasp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Engine memento implementation in filesystem.
 * 
 * @author local
 */
class AASPMementoFS implements AASPMemento {
    private final File rootDirectory;

    public AASPMementoFS(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void save(AASPEngine engine) throws IOException {
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

    private void setDefaults(AASPEngine engine) {
        // set defaults
        engine.owner = AASPEngine.DEFAULT_OWNER;
        engine.era = AASPEngine.DEFAULT_INIT_ERA;
        engine.oldestEra = AASPEngine.DEFAULT_INIT_ERA;
        engine.lastSeen = new HashMap<>();
    }

    public void restore(AASPEngine engine) throws IOException {
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
        return this.rootDirectory + "/" + AASPEngineFS.MEMENTO_FILENAME;
    }
}
