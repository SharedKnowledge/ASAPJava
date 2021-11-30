package net.sharksystem.streams;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamLink extends Thread {
    private final InputStream sourceIS;
    private final OutputStream targetOS;
    private final boolean closeStreams;
    private String id = "anon";

    public StreamLink(InputStream sourceIS, OutputStream targetOS, boolean closeStreams, String id) {
        this.sourceIS = sourceIS;
        this.targetOS = targetOS;
        this.closeStreams = closeStreams;
        this.id = id;
    }

    StreamLink(InputStream sourceIS, OutputStream targetOS, boolean closeStreams) {
        this(sourceIS, targetOS, closeStreams, "no id");
    }

    public void close() {
        this.again = false;
    }

    private boolean again = true;

    public void run() {
        Log.writeLog(this, this.toString(), "start read/write loop");
        try {
            int singleReadCounter = 0;
            int read = -1;
            do {
                int available = sourceIS.available();
                //Log.writeLog(this, this.toString(), "available: " + available);
                if (available > 0) {
                    singleReadCounter = 0;
                    byte[] buffer = new byte[available];
                    sourceIS.read(buffer);
                    targetOS.write(buffer);
                    //Log.writeLog(this, this.toString(), ASAPSerialization.printByteArrayToString(buffer)
                    //        + " end buffer:\n");
                } else {
                    // block
                    //Log.writeLog(this, this.toString(), "going to block in read(): ");
                    read = sourceIS.read();
                    if(read != -1) {
                        /*
                        Log.writeLog(this, this.toString(), "read != -1 (" + ++singleReadCounter + ")");
                        Log.writeLog(this, this.toString(), ASAPSerialization.printByteToString((short) read)
                                + " end byte");
                         */
                        targetOS.write(read);
                    } else {
                        //Log.writeLog(this, this.toString(), "read -1 - end");
                        again = false;
                    }
                }
            } while (again);
        } catch (IOException e) {
            Log.writeLog(this, this.toString(), "ioException - most probably connection closed: " + id);
        } finally {
            if(this.closeStreams) {
                Log.writeLog(this, this.toString(), "try closing linked streams: " + id);
                try {this.targetOS.close();}
                catch (IOException ioException) { Log.writeLog(this, this.toString(), "failed close input stream: " + id); }
                try {this.sourceIS.close();}
                catch (IOException ioException) { Log.writeLog(this, this.toString(), "failed close output stream: " + id); }
            }

            Log.writeLog(this, this.toString(), "end linked streams connection: " + id);
        }
    }

    public String toString() {
        return this.id;
    }
}
