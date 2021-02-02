package bugreports;

import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.apps.testsupport.ASAPMessagesMock;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Any bug reports is very welcome and appreciated.
 */
public class BugReports {
    @Test
    public void perfectSoftware() {
        System.out.println("There is no such thing as perfect software. It is only less buggy. Thanks for any help.");
    }

    /**
     * Add your report here - thank you
     */
    @Test
    public void yourBugReport() {
        System.out.println("Thanks for helping!!");
    }

    /**
     *  From ASAP Java Wiki
     *  The Test will throw an EOFException calling readUtf() on the Datainputstream at line 52
     */
    @Test
    public void messageExchangeFailure() throws Exception {
        String test = "test";
        List<byte[]> testList = new ArrayList<>();
        testList.add(test.getBytes());
        ASAPMessages message = new ASAPMessagesMock("Test", "test.uri", testList);

        Iterator<byte[]> msgIter = message.getMessages();

        if (msgIter.hasNext()) {
            byte[] yourMessageContent = msgIter.next();
            ByteArrayInputStream bais = new ByteArrayInputStream(yourMessageContent);
            DataInputStream dais = new DataInputStream(bais);

            String messageFromDais = dais.readUTF();

            assertEquals(test, messageFromDais);
        }
    }

    /**
     *  Fix for the implementation
     *  Also you cant switch Charsequence which is mentioned in your wiki
     */
    @Test
    public void messageExchangeFix() throws Exception {
        String test = "test";
        List<byte[]> testList = new ArrayList<>();
        testList.add(test.getBytes());
        ASAPMessages message = new ASAPMessagesMock("Test", "test.uri", testList);

        Iterator<byte[]> msgIter = message.getMessages();

        if (msgIter.hasNext()) {
            byte[] yourMessageContent = msgIter.next();
//            ByteArrayInputStream bais = new ByteArrayInputStream(yourMessageContent);
//            DataInputStream dais = new DataInputStream(bais);

            String messageFromDais = new String(yourMessageContent, StandardCharsets.UTF_8);

            assertEquals(test, messageFromDais);
        }
    }
}
