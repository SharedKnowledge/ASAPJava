package net.sharksystem.streams;

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
        Log.writeLog(this, "start read/write loop");
        try {
            int read = -1;
            do {
                int available = sourceIS.available();
                if (available > 0) {
                    byte[] buffer = new byte[available];
                    sourceIS.read(buffer);
                    targetOS.write(buffer);
                } else {
                    // block
                    //Log.writeLog(this, "going to block in read(): " + id);
                    read = sourceIS.read();
                    if(read != -1) {
                        targetOS.write(read);
                    } else {
                        again = false;
                    }
                }
            } while (again);
        } catch (IOException e) {
            Log.writeLog(this, "ioException - most probably connection closed: " + id);
        } finally {
            if(this.closeStreams) {
                Log.writeLog(this, "try closing linked streams: " + id);
                try {this.targetOS.close();}
                catch (IOException ioException) { Log.writeLog(this, "failed close input stream: " + id); }
                try {this.sourceIS.close();}
                catch (IOException ioException) { Log.writeLog(this, "failed close output stream: " + id); }
            }

            Log.writeLog(this, "end linked streams connection: " + id);
        }
    }
}
