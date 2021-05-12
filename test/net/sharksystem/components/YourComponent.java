package net.sharksystem.components;

import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.util.Iterator;

@ASAPFormats(formats = {YourComponent.FORMAT_A, YourComponent.FORMAT_B})
public interface YourComponent extends SharkComponent {
    String FORMAT_A = "myApp://formatA";
    String FORMAT_B = "myApp://formatB";

    void subscribeYourComponentListener(YourComponentListener aliceListener);

    void sendBroadcastMessageA(String uri, String broadcast);

    Iterator<String> getMessagesA(String uri) throws IOException, ASAPException;
}
