package net.sharksystem.asap.util;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.cmdline.CmdLineUI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Batchprocessor extends Thread {
    List<String> cmdList = new ArrayList<>();
    private CmdLineUI cmdLineUI;
    private PrintStream printStream;
    private ByteArrayInputStream inputStream;

    public Batchprocessor() {
        this(true);
    }

    public Batchprocessor(boolean cleanup) {
        if(cleanup) {
            System.out.println("clean asap peers folders");
            this.cmdLineUI = new CmdLineUI();
        }
    }

    public void addCommand(String cmd) {
        this.cmdList.add(cmd);
    }

    @Override
    public void run() {
        // prepare output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        for(String cmd : this.cmdList) {
            ps.println(cmd);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        this.printStream = System.out;
        this.inputStream = bais;

        this.cmdLineUI.runCommandLoop(this.printStream, this.inputStream);

        // in any case - give it some time to tidy up
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
