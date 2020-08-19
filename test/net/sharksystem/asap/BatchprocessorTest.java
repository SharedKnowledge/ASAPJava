package net.sharksystem.asap;

import net.sharksystem.asap.util.Batchprocessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class BatchprocessorTest {
    @Test
    public void secondMessageSentAfterReconnectTest() throws IOException, ASAPException, InterruptedException {
        Batchprocessor aliceCommands = new Batchprocessor();
        Batchprocessor bobCommands = new Batchprocessor();

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("+                           STEP 1                        +");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        // prepare
        aliceCommands.addCommand(
                "newpeer Alice\n" +
                        "newapp Alice chat\n" +
                        "newmessage Alice chat sn2://abChat HiBob\n" +
                        "open 7070 Alice");

        bobCommands.addCommand(
                "newpeer Bob\n" +
                        "newapp Bob chat\n" +
                        "newmessage Bob chat sn2://abChat HiAlice\n" +
                        "connect 7070 Bob");

        aliceCommands.executeAsThread();
        bobCommands.executeAsThread();

        // give it some time
        Thread.sleep(1000);
        // wait until ends
        aliceCommands.join();
        bobCommands.join();

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("+                           STEP 2                        +");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        // add message while online - terminate connection
        aliceCommands.addCommand(
                        "newmessage Alice chat sn2://abChat HiBob2\n" +
                        "kill all");

        aliceCommands.execute();

        // connection closed - re-establish - Bob as TCP server now
        aliceCommands.addCommand(
        "sleep 500\n" +
            "connect 7071 Alice");

        bobCommands.addCommand("open 7071 Bob");

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("+                           STEP 3                        +");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        aliceCommands.executeAsThread();
        bobCommands.executeAsThread();

        // give it some time
        Thread.sleep(1000);
        // wait until ends
        aliceCommands.join();
        bobCommands.join();

        // problem Alice' second message is not transmitted
        ASAPPeer bobPeer = bobCommands.getASAPPeer("Bob");
        ASAPEngine bobChatEngine = bobPeer.getASAPEngine("chat");
        ASAPChunkStorage receivedFromAlice = bobChatEngine.getReceivedChunksStorage("Alice");
        ASAPMessages messagesFromAlice = receivedFromAlice.getASAPMessages("sn2://abChat");
        Assert.assertEquals(2, messagesFromAlice.size());
    }
}
