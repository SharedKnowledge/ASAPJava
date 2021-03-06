package net.sharksystem.hub;

import net.sharksystem.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class TwistedConnection extends Thread {
    private final ServerSocket srv1;
    private final ServerSocket srv2;
    private final int port1;
    private final int port2;
    private final long maxIdleInMillis;

    TwistedConnection(ServerSocket srv1, ServerSocket srv2) throws IOException {
        this(srv1, srv2, 0);
    }

    TwistedConnection(ServerSocket srv1, ServerSocket srv2, int maxIdleInSeconds) throws IOException {
        this.port1 = srv1.getLocalPort();
        this.port2 = srv2.getLocalPort();
        this.srv1 = srv1;
        this.srv2 = srv2;
        this.maxIdleInMillis = maxIdleInSeconds * 1000;
        Log.writeLog(this, "going to connect peer on port " + port1 + " | " + port2);
    }

    public void run() {
        // wait for both server sockets
        Wait4AcceptThread wait4AcceptThread1 = new Wait4AcceptThread(this.srv1);
        Wait4AcceptThread wait4AcceptThread2 = new Wait4AcceptThread(this.srv2);
        wait4AcceptThread1.start();
        wait4AcceptThread2.start();

        // wait for both threads to end
        try {
            wait4AcceptThread1.join();
            wait4AcceptThread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (wait4AcceptThread1.failed != null || wait4AcceptThread2.failed != null) {
            Log.writeLog(this, "server socket accept failed: "
                    + wait4AcceptThread1.failed + " | "
                    + wait4AcceptThread2.failed
            );
        } else {
            // both side connected
            Thread twistThread1 = new TwistSocketsThread(wait4AcceptThread1.socket, wait4AcceptThread2.socket);
            Thread twistThread2 = new TwistSocketsThread(wait4AcceptThread2.socket, wait4AcceptThread1.socket);
            twistThread1.start();
            twistThread2.start();
        }
    }

    private class Wait4AcceptThread extends Thread {
        private ServerSocket wait4AcceptSocket;
        private Socket socket = null;
        private IOException failed = null;

        Wait4AcceptThread(ServerSocket wait4AcceptSocket) {
            this.wait4AcceptSocket = wait4AcceptSocket;
        }

        public void run() {
            if(maxIdleInMillis > 0) {
                new Thread() {
                    public void run() {
                        try {
                            // kill server socket after a while
                            Thread.sleep(maxIdleInMillis);
                            if(wait4AcceptSocket != null) wait4AcceptSocket.close();
                        } catch (InterruptedException | IOException e) {
                            // ignore
                        }
                    }
                }.start();
            }

            try {
                this.socket = this.wait4AcceptSocket.accept();
                //Log.writeLog(this, "server socket accepted");
                this.wait4AcceptSocket.close();
            } catch (IOException e) {
                this.failed = e;
            }
            finally {
                this.wait4AcceptSocket = null; // avoid close attempt from killer thread
            }
        }
    }

    class TwistSocketsThread extends Thread {
        private Socket sourceSocket;
        private Socket targetSocket;

        TwistSocketsThread(Socket sourceSocket, Socket targetSocket) {
            this.sourceSocket = sourceSocket;
            this.targetSocket = targetSocket;
        }

        class KillerThread extends Thread {
            @Override
            public void run() {
                try {
                    Thread.sleep(maxIdleInMillis);
                    if(sourceSocket != null) sourceSocket.close();
                    if(targetSocket != null) targetSocket.close();
                } catch (InterruptedException | IOException e) {
                    // ok
                }
            }
        }

        public void run() {
            //Log.writeLog(this, "start read/write loop");
            try {
                Thread killerThread = null;
                int read = -1;
                boolean again;
                InputStream is = this.sourceSocket.getInputStream();
                OutputStream os = this.targetSocket.getOutputStream();
                do {
                    again = false;
                    int available = is.available();
                    if(available > 0) {
                        byte[] buffer = new byte[available];
                        is.read(buffer); os.write(buffer);
                        again = true;
                    } else {
                        // set alarm clock
                        if(maxIdleInMillis > 0) {
                            killerThread = new KillerThread();
                            killerThread.start();
                        }

                        // block
                        read = is.read();
                        // back from read
                        if(killerThread != null) {
                            killerThread.interrupt(); killerThread = null;
                        }

                        os.write(read);
                        again = read != -1;
                    }
                } while (again);
            } catch (IOException e) {
                try {
                    targetSocket.close();
                } catch (IOException ioException) {
                }
            }
            finally {
                sourceSocket = null; targetSocket = null;
            }
            Log.writeLog(this, "end connection: " + port1 + " | " + port2);
        }
    }
}
