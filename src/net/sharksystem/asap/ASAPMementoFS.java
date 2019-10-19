package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;

import java.io.*;
import java.util.HashMap;

/**
 * Engine memento implementation in filesystem.
 * 
 * @author local
 */
class ASAPMementoFS implements ASAPMemento {
    private final File rootDirectory;

    public ASAPMementoFS(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void save(ASAPEngine engine) throws IOException {
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
        dos.writeUTF(engine.format);
        dos.writeInt(engine.era);
        dos.writeInt(engine.oldestEra);
        dos.writeBoolean(engine.contentChanged);
        dos.writeBoolean(engine.dropDeliveredChunks);
        dos.writeBoolean(engine.sendReceivedChunks);

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

    private void setDefaults(ASAPEngine engine) {
        // set defaults
        engine.owner = ASAPEngine.DEFAULT_OWNER;
        engine.format = ASAP_1_0.ANY_FORMAT.toString();
        engine.era = ASAPEngine.DEFAULT_INIT_ERA;
        engine.oldestEra = ASAPEngine.DEFAULT_INIT_ERA;
        engine.lastSeen = new HashMap<>();
        engine.dropDeliveredChunks = false;
        engine.sendReceivedChunks = false;
    }

    public void restore(ASAPEngine engine) throws IOException {
        String fName = this.getMementoFileName();

        File file = new File(fName);
        if(!file.exists()) {
            this.setDefaults(engine);
            return;
        }

        DataInputStream dis = new DataInputStream(
                                new FileInputStream(file));

        try {
            engine.owner = dis.readUTF();
            engine.format = dis.readUTF();
            engine.era = dis.readInt();
            engine.oldestEra = dis.readInt();
            engine.contentChanged = dis.readBoolean();
            engine.dropDeliveredChunks = dis.readBoolean();
            engine.sendReceivedChunks = dis.readBoolean();
        }
        catch(EOFException e) {
            // ignore and work with set defaults
            return; // reached end of file - nothing to do here
        }

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
        return this.rootDirectory + "/" + ASAPEngineFS.MEMENTO_FILENAME;
    }
}
