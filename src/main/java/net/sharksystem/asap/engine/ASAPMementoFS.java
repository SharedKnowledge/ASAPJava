package net.sharksystem.asap.engine;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.utils.Log;

import java.io.*;
import java.util.HashMap;

/**
 * Engine memento implementation in filesystem.
 * 
 * @author local
 */
class ASAPMementoFS implements ASAPMemento {
    private final File rootDirectory;
    private String owner;
    private String format;
    private int era;
    private int oldestEra;
    private boolean contentChanged;
    private boolean dropDeliveredChunks;
    private boolean sendReceivedChunks;
    private HashMap<String, Integer> lastSeen;
    public long lastMementoWritten;

    public ASAPMementoFS(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void save(ASAPEngine engine) throws IOException {
        /*
        Log.writeLog(this, "\n" +
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>save memento<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>save memento<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
         */
        String fName = this.getMementoFileName();

        File file = new File(fName);
        if(!file.exists()) {
            if(!file.createNewFile()) {
                throw new IOException("could not create file (problems with directory?): " + fName);
            }
        }

        DataOutputStream dos = new DataOutputStream(new FileOutputStream(fName));

        long now = System.currentTimeMillis();

        engine.lastMementoWritten = now;
        this.lastMementoWritten = engine.lastMementoWritten;
        dos.writeLong(now);
        dos.writeUTF(engine.owner);
        dos.writeUTF(engine.format);
        dos.writeInt(engine.era);
        dos.writeInt(engine.oldestEra);
        dos.writeBoolean(engine.contentChanged);
        this.contentChanged = engine.contentChanged;
        dos.writeBoolean(engine.dropDeliveredChunks);
        dos.writeBoolean(engine.routingAllowed);

        // write lastSeen hash map
        if(engine.lastSeen != null && !engine.lastSeen.isEmpty()) {
            for(String key : engine.lastSeen.keySet()) {
                Integer era = engine.lastSeen.get(key);

                // write peer and era
                dos.writeUTF(key);
                dos.writeInt(era);
            }
        }

        Log.writeLog(this, "saved: " + this);
    }

    private void setDefaults(ASAPEngine engine) {
        // set defaults
        engine.owner = ASAPEngine.DEFAULT_OWNER;
        engine.format = ASAP_1_0.ANY_FORMAT.toString();
        engine.era = ASAPEngine.DEFAULT_INIT_ERA;
        engine.oldestEra = ASAPEngine.DEFAULT_INIT_ERA;
        engine.lastSeen = new HashMap<>();
        engine.dropDeliveredChunks = false;
        engine.routingAllowed = true;
    }

    public void read() throws IOException {
        String fName = this.getMementoFileName();

        File file = new File(fName);
        if(!file.exists()) {
            return;
        }

        DataInputStream dis = new DataInputStream(
                new FileInputStream(file));

        try {
            this.lastMementoWritten = dis.readLong();
            this.owner = dis.readUTF();
            this.format = dis.readUTF();
            this.era = dis.readInt();
            this.oldestEra = dis.readInt();
            this.contentChanged = dis.readBoolean();
            this.dropDeliveredChunks = dis.readBoolean();
            this.sendReceivedChunks = dis.readBoolean();
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
                    this.lastSeen = new HashMap<>();
                    first = false;
                }

                Integer era = dis.readInt();

                // remember
                this.lastSeen.put(peer, era);
            }
        }
        catch(IOException ioe) {
            // ok  no more data
        }
        dis.close();
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
            engine.lastMementoWritten = dis.readLong();
            this.lastMementoWritten = engine.lastMementoWritten;
            engine.owner = dis.readUTF();
            engine.format = dis.readUTF();
            engine.era = dis.readInt();
            engine.oldestEra = dis.readInt();
            engine.contentChanged = dis.readBoolean();
            this.contentChanged = engine.contentChanged;
            engine.dropDeliveredChunks = dis.readBoolean();
            engine.routingAllowed = dis.readBoolean();
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
        dis.close();

        Log.writeLog(this, "restored: " + this);
    }

    private String getMementoFileName() {
        return this.rootDirectory + "/" + ASAPEngineFS.MEMENTO_FILENAME;
    }

    public String getFormat() {
        return this.format;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("changed == ");
        sb.append(this.contentChanged);
        sb.append(" | written == ");
        sb.append(this.lastMementoWritten);

        return sb.toString();
    }
}
