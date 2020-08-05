package net.sharksystem.asap;

import net.sharksystem.asap.util.Batchprocessor;
import org.junit.Test;

import java.io.IOException;

public class BatchprocessorTest {
    @Test
    public void secondMessageNotSendTest() throws IOException, ASAPException, InterruptedException {
        Batchprocessor aliceCommands = new Batchprocessor();
        Batchprocessor bobCommands = new Batchprocessor();
        aliceCommands.addCommand(
                "newpeer Alice\n" +
                        "newapp Alice chat\n" +
                        "newmessage Alice chat sn2://abChat HiBob\n" +
                        "connect 7070 Alice\n" +
                        "sleep 2000\n" +
                        "newmessage Alice chat sn2://abChat HiBob2\n" +
                        "sleep 1000\n" +
                        "kill all\n" +
                        "open 7071 Alice\n" +
                        "sleep 5000"
                        );


        bobCommands.addCommand(
                "newpeer Bob\n" +
                        "newapp Bob chat\n" +
                        "newmessage Bob chat sn2://abChat HiAlice\n" +
                        "open 7070 Bob\n" +
                        "sleep 1000\n" +
                        "connect 7071 Bob\n" +
                        "sleep 1000"
        );

        // lets go
        aliceCommands.start();
        bobCommands.start();

        aliceCommands.join();
        bobCommands.join();
    }
}
