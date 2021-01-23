package net.sharksystem;

import net.sharksystem.asap.ASAPEnvironmentChangesListenerManagement;
import net.sharksystem.asap.ASAPMessageReceivedListenerManagement;
import net.sharksystem.asap.ASAPPeer;

/**
 * A Shark (Shared Knowledge) Peer is considered to be a collection of ASAP apps.
 * The interfaces and some little classes are meant to be a template how to put different
 * ASAP apps together into a single application - a Shark (Shared Knowledge) application.
 */
public interface SharkPeer {
    /**
     * Add a component to the Shark app
     * @param componentFactory
     * @throws SharkException a) wrong status. Peer is already running. b) there is already a
     * component supporting same format.
     */
    void addComponent(SharkComponentFactory componentFactory) throws SharkException;

    /**
     * Remove a component from your Shark application
     * @param component
     * @throws SharkException a) wrong status. Peer is already running. b) there is no component
     * for this format
     */
    void removeComponent(SharkComponent component) throws SharkException;

    /**
     *
     * @param format
     * @return component supporting this format.
     * @throws SharkException No component supports this format
     */
    SharkComponent getComponent(CharSequence format) throws SharkException;

    /**
     * Start the Shark peer. An ASAP peer will be launched with listening to all format from all
     * components. Components can neither be added nor withdrawn after launch.
     * @throws SharkException Exception can only be caused by ASAP peer launch
     */
    void start() throws SharkException;

    /**
     * Stop that peer. ASAP peer will be stopped.
     * @throws SharkException
     */
    void stop() throws SharkException;

    /**
     *
     * @return current status.
     */
    SharkPeerStatus getStatus();

    /**
     * @return reference to the ASAPPeer object. It is a good idea to use this interface to get that reference
     * when using your ASAP app as Shark component.
     * @throws SharkException e.g. from status - Shark Peer not yet started
     */
    ASAPPeer getASAPPeer() throws SharkException;
}
