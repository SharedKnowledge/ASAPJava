package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPInternalPeer;
import net.sharksystem.asap.cmdline.CmdLineUI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Batchprocessor implements Runnable {
    List<String> cmdList = new ArrayList<>();
    private CmdLineUI cmdLineUI;
    private PrintStream printStream;
    private ByteArrayInputStream inputStream;
    private Thread runningThread;

    public Batchprocessor() {
        this(true);
    }

    public Batchprocessor(boolean cleanup) {
        if(cleanup) {
            System.out.println("clean asap peers folders");
            this.cmdLineUI = new CmdLineUI();
        }
    }

    public void setOutputstream(PrintStream ps) {
        this.cmdLineUI.setOutStreams(ps);
    }

    public void addCommand(String cmd) {
        this.cmdList.add(cmd);
    }

    public void execute() {
        this.prepareExecution();
        this.doExecution();
    }

    public void executeAsThread() {
        this.prepareExecution();
        this.runningThread = new Thread(this);
        this.runningThread.start();
    }

    public void join() throws InterruptedException {
        if(this.runningThread != null && this.runningThread.isAlive()) {
            this.runningThread.join();
        }
    }

    private void prepareExecution() {
        // prepare output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        for(String cmd : this.cmdList) {
            ps.println(cmd);
        }

        // clean cmd list
        this.cmdList = new ArrayList<>();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        this.printStream = System.out;
        this.inputStream = bais;
    }

    private void doExecution() {
        this.cmdLineUI.runCommandLoop(this.printStream, this.inputStream);

        // in any case - give it some time to tidy up
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }

        this.runningThread = null;
    }

    @Override
    public void run() {
        this.doExecution();
    }

    public ASAPInternalPeer getASAPPeer(String peerName) throws ASAPException {
        return this.cmdLineUI.getASAPPeer(peerName);
    }
}
