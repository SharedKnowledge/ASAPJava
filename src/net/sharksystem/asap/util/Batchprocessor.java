package net.sharksystem.asap.util;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.cmdline.CmdLineUI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Batchprocessor {
    private final boolean cleanup;
    List<String> cmdList = new ArrayList<>();

    public Batchprocessor() {
        this(true);
    }

    public Batchprocessor(boolean cleanup) {
        this.cleanup = cleanup;
    }

    public void addCommand(String cmd) {
        this.cmdList.add(cmd);
    }

    public void execute() throws IOException, ASAPException {
        // prepare output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        for(String cmd : this.cmdList) {
            ps.println(cmd);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        CmdLineUI cmdLineUI = new CmdLineUI(System.out, bais);
        if(this.cleanup) {
            System.out.println("clean asap peers folders");
            cmdLineUI.doResetASAPStorages();
        }

        cmdLineUI.runCommandLoop();
    }
}
