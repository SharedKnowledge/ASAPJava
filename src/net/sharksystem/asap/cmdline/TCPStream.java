package net.sharksystem.asap.cmdline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author thsc
 */
public class TCPStream extends Thread {
    private final int port;
    private final boolean asServer;
    private final String name;
    private TCPStreamCreatedListener listener = null;
    private Socket socket = null;
    
    private boolean fatalError = false;

    public final int WAIT_LOOP_IN_MILLIS = 1000; // 30 sec
    private Thread createThread = null;
    private TCPServer tcpServer = null;
    private TCPClient tcpClient = null;
    private long waitInMillis = WAIT_LOOP_IN_MILLIS;

    public TCPStream(int port, boolean asServer, String name, TCPStreamCreatedListener listener) {
        this.port = port;
        this.asServer = asServer;
        this.name = name;
        this.listener = listener;
    }

    public TCPStream(int port, boolean asServer, String name) {
        this(port, asServer, name, null);
    }

    public void setListener(TCPStreamCreatedListener listener) {
        this.listener = listener;
    }

    public void setWaitPeriod(long waitInMillis) {
        this.waitInMillis = waitInMillis;
    }

    public void kill() {
        try {
            if(this.socket != null) {
                this.socket.close();
            }

            if(this.tcpClient != null) {
                this.tcpClient.kill();
            }

            if(this.tcpServer != null) {
                    this.tcpServer.kill();
            }
        } catch (IOException e) {
            System.err.println("TCPChannel: problems while killing: " + e.getLocalizedMessage());
        }
    }
    
    @Override
    public void run() {
        this.createThread = Thread.currentThread();
        try {
            if(this.asServer) {
                System.out.println(
                        "TCPChannel: note: this implementation will only accept *one* connection attempt as server");
                this.tcpServer = new TCPServer();
                this.socket = tcpServer.getSocket();
            } else {
                this.tcpClient = new TCPClient();
                this.socket = this.tcpClient.getSocket();
            }

            // we have got a socket
            if(this.listener != null) {
                this.listener.streamCreated(this);
            }
        } catch (IOException ex) {
            //<<<<<<<<<<<<<<<<<<debug
            String s = "couldn't establish connection";
            System.out.println(s);
            this.fatalError = true;
        }
    }
    
    public void close() throws IOException {
        if(this.socket != null) {
            this.socket.close();
            //<<<<<<<<<<<<<<<<<<debug
            System.out.println("socket closed");
        }
    }

    /**
     * holds thread until a connection is established
     */
    public void waitForConnection(long time2wait) throws IOException {
        this.setWaitPeriod(time2wait);
        this.waitForConnection();
    }

    /**
     * holds thread until a connection is established
     */
    public void waitForConnection() throws IOException {
        if(this.createThread == null) {
            /* in unit tests there is a race condition between the test
            thread and those newly created tests to establish a connection.
            
            Thus, this call could be in the right order - give it a
            second chance
            */
            
            try {
                Thread.sleep(this.waitInMillis);
            } catch (InterruptedException ex) {
                // ignore
            }

            if(this.createThread == null) {
                // that's probably wrong usage:
                throw new IOException("must start TCPChannel thread first by calling start()");
            }
        }
        
        
        while(!this.fatalError && this.socket == null) {
            try {
                Thread.sleep(this.waitInMillis);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
    }
    
    public void checkConnected() throws IOException {
        if(this.socket == null) {
            //<<<<<<<<<<<<<<<<<<debug
            String s = "no socket yet - should call connect first";
            System.out.println(s);
            //>>>>>>>>>>>>>>>>>>>debug
            throw new IOException(s);
        }
    }
    
    public InputStream getInputStream() throws IOException {
        this.checkConnected();
        return this.socket.getInputStream();
    }
    
    public OutputStream getOutputStream() throws IOException {
        this.checkConnected();
        return this.socket.getOutputStream();
    }
    
    private class TCPServer {
        private ServerSocket srvSocket = null;

        Socket getSocket() throws IOException {
            if(this.srvSocket == null) {
                this.srvSocket = new ServerSocket(port);
            }

            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append("TCPChannel (");
            b.append(name);
            b.append("): ");
            b.append("opened port ");
            b.append(port);
            b.append(" on localhost and wait");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            Socket socket = this.srvSocket.accept();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("TCPChannel (");
            b.append(name);
            b.append("): ");
            b.append("connected");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            
            return socket;
        }

        public void kill() throws IOException {
            this.srvSocket.close();
        }
    }

    private class TCPClient {
        private boolean killed = false;

        public void kill() {
            this.killed = true;
        }

        Socket getSocket() throws IOException {
            while(!this.killed) {
                try {
                    //<<<<<<<<<<<<<<<<<<debug
                    StringBuilder b = new StringBuilder();
                    b.append("TCPChannel (");
                    b.append(name);
                    b.append("): ");
                    b.append("try to connect localhost port ");
                    b.append(port);
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                    Socket socket = new Socket("localhost", port);
                    return socket;
                }
                catch(IOException ioe) {
                    //<<<<<<<<<<<<<<<<<<debug
                    StringBuilder b = new StringBuilder();
                    b.append("TCPChannel (");
                    b.append(name);
                    b.append("): ");
                    b.append("failed / wait and re-try");
                    b.append(port);
                    System.out.println(b.toString());
                    try {
                        Thread.sleep(waitInMillis);
                    } catch (InterruptedException ex) {
                        // ignore
                    }
                }
            }
            throw new IOException("thread was killed before establishing a connection");
        }
    }
}
