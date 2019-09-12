package net.sharksystem.util.localloop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author thsc
 */
public class TCPChannel extends Thread {
    private final int port;
    private final boolean asServer;
    private final String name;
    private Socket socket = null;
    
    private boolean fatalError = false;
    private boolean threadRunning = false;
    
    public final int WAIT_LOOP_IN_MILLIS = 1000;
    
    public TCPChannel(int port, boolean asServer, String name) {
        this.port = port;
        this.asServer = asServer;
        this.name = name;
    }
    
    @Override
    public void run() {
        this.threadRunning = true;
        try {
        if(this.asServer) {
            this.socket = new TCPChannel.TCPServer().getSocket();
        } else {
            this.socket = new TCPChannel.TCPClient().getSocket();
        }
        } catch (IOException ex) {
            //<<<<<<<<<<<<<<<<<<debug
            String s = "couldn't esatblish connection";
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
    public void waitForConnection() throws IOException {
        if(!this.threadRunning) {
            /* in unit tests there is a race condition between the test
            thread and those newly created tests to establish a connection.
            
            Thus, this call could be in the right order - give it a
            second chance
            */
            
            try {
                Thread.sleep(WAIT_LOOP_IN_MILLIS);
            } catch (InterruptedException ex) {
                // ignore
            }

            if(!this.threadRunning) {
                // that's probably wrong usage:
                throw new IOException("must start TCPChannel thread first by calling start()");
            }
        }
        
        
        while(!this.fatalError && this.socket == null) {
            try {
                Thread.sleep(WAIT_LOOP_IN_MILLIS);
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

    public InputStream getInputStream(PrintStream logger, String preamble) throws IOException {
        this.checkConnected();
        InputStream is = this.socket.getInputStream();
        if(logger == null) {
            return is;
        }

        return new TCPChannel.InputStreamLogger(is, logger, preamble);
    }

    public InputStream getInputStream() throws IOException {
        return this.getInputStream(null, null);
    }

    public OutputStream getOutputStream(PrintStream logger, String preamble) throws IOException {
        this.checkConnected();
        OutputStream os = this.socket.getOutputStream();
        if(logger == null) {
            return os;
        }

        return new TCPChannel.OutputStreamLogger(os, logger, preamble);
    }

    public OutputStream getOutputStream() throws IOException {
        return this.getOutputStream(null, null);
    }

    private class InputStreamLogger extends InputStream {
        private final InputStream is;
        private final PrintStream ps;
        private final String preamble;

        InputStreamLogger(InputStream is, PrintStream ps, String preamble) {
            this.is = is;
            this.ps = ps;
            this.preamble = preamble != null ? preamble : "";
        }

        @Override
        public int read() throws IOException {
            int value = this.is.read();
            writeLog(this.ps, value, this.preamble + "r:");

            return value;
        }
    }

    private class OutputStreamLogger extends OutputStream {
        private final OutputStream os;
        private final PrintStream ps;
        private final String preamble;

        OutputStreamLogger(OutputStream os, PrintStream ps, String preamble) {
            this.os = os;
            this.ps = ps;
            this.preamble = preamble != null ? preamble : "";
        }

        @Override
        public void write(int value) throws IOException {
            writeLog(this.ps, value, this.preamble + "w:");
            this.os.write(value);
        }
    }

    private static int logCounter = 0;
    private static void writeLog(PrintStream ps, int value, String pre) {
        ps.printf(pre + "%x, ", value);
        logCounter++;
        if(logCounter == 10) {
            ps.print("\n");
            logCounter = 0;
        }
    }

    private class TCPServer {
        Socket getSocket() throws IOException {
            ServerSocket srvSocket = new ServerSocket(port);
            
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

            Socket socket = srvSocket.accept();
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
    }

    private class TCPClient {
        Socket getSocket() throws IOException {
            for(;;) {
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
                        Thread.sleep(WAIT_LOOP_IN_MILLIS);
                    } catch (InterruptedException ex) {
                        // ignore
                    }
                }
            }
        }
    }
}
