/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sharksystem.asp3;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author thsc
 */
public interface ASP3Chunk {
    /**
     * 
     * @return number of message in that chunk
     */
    int getNumberMessage();
    
    /**
     * URI which represents topic of messages in that chunk
     * @return
     * @throws IOException 
     */
    String getUri() throws IOException;

    /**
     * 
     * @return iterator of all messages in the chunk
     * @throws IOException 
     */
    Iterator<CharSequence> getMessages() throws IOException;

    /**
     * remove that chunk.. drop all object references after
     * calling this methods. Further calls on this object
     * have an undefined behaviour.
     */
    public void drop();
    
    public int getEra() throws IOException;
}
