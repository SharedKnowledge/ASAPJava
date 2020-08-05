package net.sharksystem.asap;

import net.sharksystem.asap.util.Batchprocessor;
import org.junit.Test;

import java.io.IOException;

public class BatchprocessorTest {
    @Test
    public void basicTest() throws IOException, ASAPException {
        Batchprocessor batchprocessor = new Batchprocessor();
        batchprocessor.addCommand("newpeer Clara");

        batchprocessor.addCommand(
                "newapp Clara chat\n" +
                "newmessage Clara chat sn2://abChat HiBob");

        batchprocessor.execute();
    }
}
