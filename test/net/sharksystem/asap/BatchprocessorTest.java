package net.sharksystem.asap;

import net.sharksystem.asap.util.Batchprocessor;
import org.junit.Test;

import java.io.IOException;

public class BatchprocessorTest {
    @Test
    public void basicTest() throws IOException, ASAPException {
        Batchprocessor claraCommands = new Batchprocessor();

        claraCommands.addCommand(
                "resetstorage\n" +
                        "newpeer Alice\n" +
                        "newapp Alice chat\n" +
                        "sleep 1000");
        claraCommands.execute();
    }
}
