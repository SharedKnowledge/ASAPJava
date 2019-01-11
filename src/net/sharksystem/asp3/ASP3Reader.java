package net.sharksystem.asp3;

/**
 * Developers have to implement a class implementing that interface
 * before using that framework. ASP3 is menat to store and forward
 * messages in dynamic ad-hoc networks. ASP3 does not process those
 * messages - that's applications specific.
 * 
 * That interfaces is used by ASP3Engine as callback interface. The
 * engine retrieves message from other peers and calls methods declared
 * in that interface. 
 *
 * Developers must implement that callback class before using that framework
 * 
 * @see ASP3Engine
 * @see ASP3ChunkStorage
 * @author thsc
 */
public interface ASP3Reader {
    
    /**
     * called when a new sender was detected. Following read message are
     * produced by this sender.
     * 
     * @param sender 
     */
    public void newSender(String sender);
    
    /**
     * a new message was read from an external sender
     * @param urlTarget
     * @param message 
     */
    public void read(String urlTarget, String message);
}
