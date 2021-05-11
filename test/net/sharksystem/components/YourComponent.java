package net.sharksystem.components;

import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;

@ASAPFormats(formats = {YourComponent.FORMAT_A, YourComponent.FORMAT_B})
public interface YourComponent extends SharkComponent {
    String FORMAT_A = "myApp://formatA";
    String FORMAT_B = "myApp://formatB";

    void subscribeYourComponentListener(YourComponentListener aliceListener);

    void sendBroadcastMessageA(String uri, String broadcast);
}
