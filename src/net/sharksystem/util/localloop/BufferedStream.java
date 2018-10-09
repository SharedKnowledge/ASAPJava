package net.sharksystem.util.localloop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author thsc
 */
public class BufferedStream {
    public static final int BUFFER_SIZE = 10000;
    private byte[] buffer = new byte[BUFFER_SIZE];
    
    private int writeIndex = 0; // next index to write to
    private int readIndex = 0; // next index to read from
    
    private boolean bufferEmpty = true;
    
    private Thread readerThread = null;
    private Thread writerThread = null;
    
    private boolean disconnected = false;
    private boolean eofSubmitted = false;
    private boolean eofRead = false;
    private final String debugName;
    
    public BufferedStream(String debugName) {
        this.debugName = debugName;
    }
    
    public BufferedStream() {
        this.debugName = "buffer stream";
    }
    
    /**
     * Write byte into a buffer
     * @param nextByte byte to write
     */
    private synchronized void writeToBuffer(int nextByte) throws IOException {
        //<<<<<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("Log (");  b.append(this.debugName); b.append("): ");
        b.append("entered write with next byte: ");
        b.append(nextByte);
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>>>>debug
        
        this.checkConnection();
        
        // detect EOF
        if(nextByte < 0) {
            this.eofRead = true;
            return;
        }
        
        /*
        Buffer full? write index must never overtake read index.
        Problem: 
        Overtaking (buffer full) means: write index == read index.
        Buffer empty has same feature. Thus, we have to remember
        if buffer full or empty.
        */
        
        //<<<<<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append("Log (");  b.append(this.debugName); b.append("): ");
        b.append("have to wait? ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>>>>debug
        
        if(!this.bufferEmpty && this.writeIndex == this.readIndex) {
            // buffer full - wait until data read
            this.writerThread = Thread.currentThread();
            //<<<<<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("Log (");  b.append(this.debugName); b.append("): ");
            b.append("wait in write");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>>>>debug
            while(!this.bufferEmpty && this.writeIndex == this.readIndex) {
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException ex) {
                    // woke up - very good - try again
                    //<<<<<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append("Log (");  b.append(this.debugName); b.append("): ");
                    b.append("woke up by interrupt ");
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>>>>debug
                }
            }
            this.writerThread = null;
        }

        this.buffer[this.writeIndex] = (byte) nextByte;
        
        this.incIndexSyncThreads(true);
    }
    
    private int nextIndex(int index) {
        index++;
        if(index >= BufferedStream.BUFFER_SIZE) {
            index = 0;
        }
        
        return index;
    }

    private synchronized void incIndexSyncThreads(boolean writer) {
        StringBuilder b;
        
        if(writer) {
            this.writeIndex = this.nextIndex(this.writeIndex);
            
            // buffer not empty yet. We have written something a moment ago.
            this.bufferEmpty = false;
            
            // wake up reader - if any
            if(this.readerThread != null) {
                this.readerThread.interrupt();
                
                b = new StringBuilder();
                b.append("Log (");
                b.append(this.debugName);
                b.append("):");
                b.append("interrupted waiting reader");
                System.out.println(b.toString());
            }
            
        } else {
            this.readIndex = this.nextIndex(this.readIndex);
            
            // read index has reached write index? buffer empty
            if(this.readIndex == this.writeIndex) {
                this.bufferEmpty = true;
            }
            
            // wake up writer - if any
            if(this.writerThread != null) {
                this.writerThread.interrupt();
                b = new StringBuilder();
                b.append("Log (");
                b.append(this.debugName);
                b.append("):");
                b.append("interrupted waiting writer");
                System.out.println(b.toString());
            }
        }
        
        b = new StringBuilder();
        b.append("Log (");
        b.append(this.debugName);
        b.append("): read/write/empty: ");
        b.append(this.readIndex);
        b.append(" / ");
        b.append(this.writeIndex);
        b.append(" / ");
        b.append(this.bufferEmpty);
        
        System.out.println(b.toString());
    }
    
    private void checkConnection() throws IOException {
        if(this.disconnected) {
            throw new IOException("connection closed");
        }
        if(this.eofSubmitted) {
            throw new IOException("eof already reached");
        }
        
    }
    
    /**
     * Reader comes in an wants to get a byte
     * @return a byte, -1 indicating EOF 
     * @throws IOException if connection was closed or broken
     */
    private synchronized int readFromBuffer() throws IOException {
        //<<<<<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("Log (");  b.append(this.debugName); b.append("): ");
        b.append("entered read");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>>>>debug
        
        this.checkConnection();
        
        if(this.eofRead) {
            this.eofSubmitted = true;
            return -1;
        }
        
        // Read index equals write index: buffer is empty
        if(this.bufferEmpty) {
            //<<<<<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("Log (");  b.append(this.debugName); b.append("): ");
            b.append("buffer empty");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>>>>debug

            // queue in wake up slot
            this.readerThread = Thread.currentThread();
            
            // sleep until at least a single byte in buffer
            while(this.bufferEmpty) {
                //<<<<<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append("Log (");  b.append(this.debugName); b.append("): ");
                b.append("wait in read");
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>>>>debug
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException ex) {
                    // woke up - very good - try again
                    //<<<<<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append("Log (");  b.append(this.debugName); b.append("): ");
                    b.append("woke up");
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>>>>debug
                }
            }
            
            // remove from wake up list
            this.readerThread = null;
        }
        
        // we have something to read
        byte retval = this.buffer[this.readIndex];
        
        this.incIndexSyncThreads(false);

        return retval;
    }
    
    private void close() {
        this.disconnected = true;
    }
    
    private InputStream consumerIS = null;
    public InputStream getInputStream() {
        if(this.consumerIS == null) {
            this.consumerIS = new BufferedStream.InputStreamConnector(this);
        }
        
        return this.consumerIS;
    }
    
    private OutputStream producerOS = null;
    public OutputStream getOutputStream() {
        if(this.producerOS == null) {
            this.producerOS = new BufferedStream.OutputStreamConnector(this);
        }
        
        return this.producerOS;
    }
    
    private class InputStreamConnector extends InputStream {
        private final BufferedStream buffer;
        
        InputStreamConnector(BufferedStream isBuffer) {
            this.buffer = isBuffer;
        }

        @Override
        public int read() throws IOException {
            return this.buffer.readFromBuffer();
        }
        
        @Override
        public void close() throws IOException {
            super.close();
            this.buffer.close();
        }
    }
    
    private class OutputStreamConnector extends OutputStream {
        private final BufferedStream buffer;
        
        OutputStreamConnector(BufferedStream osBuffer) {
            this.buffer = osBuffer;
        }

        @Override
        public void write(int b) throws IOException {
            this.buffer.writeToBuffer(b);
        }
        
        @Override
        public void close() throws IOException {
            super.close();
            this.buffer.close();
        }
    }
}
