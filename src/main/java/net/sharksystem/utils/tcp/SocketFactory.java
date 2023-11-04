package net.sharksystem.utils.tcp;

//import net.sharksystem.asap.ASAPEncounterHelper;

import net.sharksystem.utils.Log;
import net.sharksystem.utils.streams.StreamPairImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketFactory implements Runnable {
    private final ServerSocket srv;
    private int port = 0;
    private StreamPairCreatedListener listener = null;
    InputStream is;
    OutputStream os;
    private Thread waitForConnectionThread = null;
    private String remoteAddress;

    public SocketFactory(int portNumber, StreamPairCreatedListener listener) throws IOException {
        this(new ServerSocket(portNumber));
        this.port = portNumber;
        this.listener = listener;
    }

    public SocketFactory(ServerSocket srv) {
        this.srv = srv;
    }

    /**
     * Close server socket - kills thread already running
     */
    public void close() throws IOException {
        Log.writeLog(this, "close TCP server socket - do long longer accept connection attempts on port: " + this.port);
        this.srv.close();
    }

    private boolean running = false;
    @Override
    public void run() {
        this.running = true;
        Log.writeLog(this,"socket factory running - accept connections on port: " + this.port);
        try {
            Socket socket = srv.accept();
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            this.remoteAddress = SocketFactory.getRemoteAddress(socket);
            Log.writeLog(this,"connection attempt accepted: socket created");
            if(this.waitForConnectionThread != null) {
                //this.waitForConnectionThread.interrupt();
                this.waitForConnectionThread.notify();
            }
            if(this.listener != null) {
                this.listener.streamPairCreated(
                        StreamPairImpl.getStreamPairWithEndpointAddress(this.is, this.os, this.remoteAddress));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream getInputStream() throws IOException {
        if (this.is == null) {
            if(!running) {
                throw new IOException("start factory thread first");
            }
            this.waitForConnectionThread = Thread.currentThread();
            try {
                //Thread.sleep(Long.MAX_VALUE);
                this.waitForConnectionThread.wait();
            } catch (InterruptedException e) {
                // great - do it again
                return this.getInputStream();
            }
        }

        return this.is;
    }

    public OutputStream getOutputStream() throws IOException {
        if (this.os == null) {
            if(!running) {
                throw new IOException("start factory thread first");
            }
            this.waitForConnectionThread = Thread.currentThread();
            try {
                //Thread.sleep(Long.MAX_VALUE);
                this.waitForConnectionThread.wait();
            } catch (InterruptedException e) {
                // great - do it again
                return this.getOutputStream();
            }
        }

        return this.os;
    }

    public String getRemoteAddress() throws IOException {
        if (this.os == null) {
            if(!running) {
                throw new IOException("start factory thread first");
            }
            this.waitForConnectionThread = Thread.currentThread();
            try {
                //Thread.sleep(Long.MAX_VALUE);
                this.waitForConnectionThread.wait();
            } catch (InterruptedException e) {
                // great - do it again
            }
        }

        return this.remoteAddress;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                           other useful stuff                                          //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final String getRemoteAddress(Socket connectedSocket) throws IOException {
        InetAddress inetAddress = connectedSocket.getInetAddress();
        int port = connectedSocket.getPort();
        return inetAddress.getHostAddress() + ":" + port;
    }

}
