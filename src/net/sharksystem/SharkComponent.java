package net.sharksystem;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;

import java.io.IOException;

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

    /**
     * Peer exchange data during an encounter. Components can show a specific behaviour. They could
     * send a special greeting message whenever a unknown peer shows up. Such a behaviour can easily
     * be implemented with a component. It is just a send message call on the ASAPPeer.
     * <br/>
     * We strongly suggest to implement such methods instead of asking component users to do so. We also strongly
     * suggest to give component users means to define an appropriate for their specific applications.
     * <br/>
     * That is what that method is for. Define a set of constants in your component interface. Write
     * a good documentation. Now, users can switch on or off a well-defined and -documented behaviour on your component.
     *
     * @param behaviourName
     * @param on
     * @see ASAPPeer#sendASAPMessage(CharSequence, CharSequence, byte[])
     */
    void setBehaviour(String behaviourName, boolean on) throws SharkUnknownBehaviourException, ASAPException, IOException;
}
