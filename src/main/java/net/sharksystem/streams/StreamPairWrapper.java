package net.sharksystem.streams;

import net.sharksystem.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamPairWrapper extends StreamPairListenerManager implements StreamPair {
    private final InputStreamWrapper is;
    private final OutputStreamWrapper os;
    private final String endpointAddress;

    /**
     * @param is
     * @param os
     * @param listener
     * @param endpointAddress
     */
    public StreamPairWrapper(InputStream is, OutputStream os, WrappedStreamPairListener listener, String endpointAddress) {
        this.is = new InputStreamWrapper(is);
        this.os = new OutputStreamWrapper(os);
        this.endpointAddress = endpointAddress;
        super.addListener(listener);

    }

    public StreamPairWrapper(InputStream is, OutputStream os) {
        this(is, os, null, "0");
    }

    @Override
    public InputStream getInputStream() {
        return this.is;
    }

    @Override
    public OutputStream getOutputStream() {
        return this.os;
    }

    @Override
    public void close() {
        // do not close the streams but prevent any further communication
        Log.writeLog(this, "closed");
        this.is.closed = true;
        this.os.closed = true;
        this.notifyAllListenerClosed(this, this.endpointAddress);
        /*
        if(!this.listenerList.isEmpty()) {
            for(WrappedStreamPairListener listener : this.listenerList) {
                listener.notifyClosed(this.id);
            }
        }
         */
    }

    @Override
    public CharSequence getEndpointAddress() {
        return this.endpointAddress;
    }

    @Override
    public CharSequence getSessionID() {
        return StreamPairImpl.NO_ID;
    }

    private void notifyAction() {
        if(!this.listenerList.isEmpty()) {
            for(StreamPairListener listener : this.listenerList) {
                if(listener instanceof WrappedStreamPairListener) {
                    WrappedStreamPairListener wrappedListener = (WrappedStreamPairListener) listener;
                    wrappedListener.notifyAction(this.endpointAddress);
                }
            }
        }
    }

    public void addListener(WrappedStreamPairListener listener) {

    }

    private class InputStreamWrapper extends InputStream {
        private final InputStream is;
        private boolean closed = false;

        InputStreamWrapper(InputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            if(this.closed) throw new IOException("wrapped stream closed");
            int i = this.is.read();
            if(this.closed) {
                Log.writeLog(this, "read sign after already closed " + i);
                throw new IOException("wrapped stream closed");
            }
            // else
            StreamPairWrapper.this.notifyAction();
            return i;
        }

        public void close() {
            StreamPairWrapper.this.close();
        }
    }

    private class OutputStreamWrapper extends OutputStream {
        private final OutputStream os;
        private boolean closed = false;

        OutputStreamWrapper(OutputStream os) {
            this.os = os;
        }

        @Override
        public void write(int value) throws IOException {
            if(this.closed) throw new IOException("wrapped stream closed");
            this.os.write(value);
            StreamPairWrapper.this.notifyAction();
        }

        public void close() {
            StreamPairWrapper.this.close();
        }
    }

    public String toString() {
        return this.endpointAddress;
    }
}
