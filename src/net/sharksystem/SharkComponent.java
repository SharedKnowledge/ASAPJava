package net.sharksystem;

import net.sharksystem.asap.ASAPPeer;

/**
 * It is nothing but a tag. Let your interface extends this to make clear what is is:
 * A functional interface of your component.
 *
 * The ASAPFormats annotation is mandatory. It must not be null.
 * There are a view (or even just one) reserved format used by ASAP itself.
 * Do not use any of those formats.
 *
 */
@ASAPFormats(formats = {})
public interface SharkComponent {
    /**
     * Do not use any of those formats with your application
     */
    CharSequence[] reservedFormats = new CharSequence[]{
            net.sharksystem.asap.management.ASAPManagementStorage.ASAP_CREATE_CHANNEL
        };


    /**
     * This method is called on your component after the Shark Peer and its ASAP peer has been initialized
     * and started. You can ignore this call or you can do some initialization stuff. It is a good place
     * to add listeners, e.g. message listeners to the ASAPPeer.
     * @param peer ASAPPeer
     * @throws SharkException
     */
    void onStart(ASAPPeer peer) throws SharkException;
}
