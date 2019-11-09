package net.sharksystem.asap;

import net.sharksystem.asap.apps.ASAPJavaApplication;
import net.sharksystem.asap.apps.ASAPJavaApplicationFS;
import net.sharksystem.asap.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.apps.ASAPMessages;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

public class ASAPJavaApplicationTests {
    public static final String ALICE = "Alice";
    public static final String BOB = "Bob";
    public static final String ALICE_ROOT_FOLDER = "tests/Alice";
    private static final CharSequence ALICE_APP_FORMAT = "TEST_FORMAT";
    private static final byte[] TESTMESSAGE = "TestMessage".getBytes();

    @Test
    public void usageTest() throws IOException, ASAPException {
        Collection<CharSequence> formats = new HashSet<>();
        formats.add(ALICE_APP_FORMAT);

        ASAPJavaApplication asapJavaApplication =
                ASAPJavaApplicationFS.createASAPJavaApplication(ALICE, ALICE_ROOT_FOLDER, formats);

        Collection<CharSequence> recipients = new HashSet<>();
        recipients.add(BOB);

        asapJavaApplication.sendASAPMessage(ALICE_APP_FORMAT, "yourSchema://yourURI", recipients, TESTMESSAGE);

        asapJavaApplication.setASAPMessageReceivedListener(ALICE_APP_FORMAT, new ListenerExample());
    }

    private class ListenerExample implements ASAPMessageReceivedListener {

        @Override
        public void asapMessagesReceived(ASAPMessages messages) {
            try {
                System.out.println("#message == " + messages.size());
            } catch (IOException e) {
                // do something with it.
            }
        }
    }
}
