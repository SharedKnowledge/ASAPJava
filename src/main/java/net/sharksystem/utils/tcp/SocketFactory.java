package net.sharksystem.utils.tcp;

//import net.sharksystem.asap.ASAPEncounterHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketFactory implements Runnable {
    private final ServerSocket srv;
    InputStream is;
    OutputStream os;
    private Thread waitForConnectionThread = null;
    private String remoteAddress;

    public SocketFactory(ServerSocket srv) {
        this.srv = srv;
    }

    private boolean running = false;
    @Override
    public void run() {
        this.running = true;
        System.out.println("socket factory running");
        try {
            Socket socket = srv.accept();
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            this.remoteAddress = SocketFactory.getRemoteAddress(socket);
            System.out.println("socket created");
            if(this.waitForConnectionThread != null) {
                //this.waitForConnectionThread.interrupt();
                this.waitForConnectionThread.notify();
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
