package net.sharksystem;

import net.sharksystem.asap.ASAPPeer;

import java.util.Set;

public interface SharkComponent {
    /**
     * @return all format supported by your application. It is most probably a good idea to use
     * the same implementation for this message here and in the factory.
     */
    Set<CharSequence> getSupportedFormats();

    /**
     * @return Interface that offers components' methods. It might look a bit strange and actually it is.
     * Returning an interface that has no methods and is only used as a tag to declare an interface (which
     * is actually a facade). But it give a single template, a frame how to get a reference of this
     * Shark component. You have to cast this interface to the real one.
     */
    SharkComponentInterface getInterface();

    /**
     * This method is called on your component after the Shark Peer and its ASAP peer has been initialized
     * and started. You can ignore this call or you can do some initialization stuff. It is a good place
     * to add listeners, e.g. message listeners to the ASAPPeer.
     * @param peer ASAPPeer
     * @throws SharkException
     */
    void onStart(ASAPPeer peer) throws SharkException;
}
