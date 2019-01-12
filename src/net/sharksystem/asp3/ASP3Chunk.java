/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sharksystem.asp3;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
    
    /**
     * 
     * @return recipients of that chunk 
     */
    List<CharSequence> getRecipients();

    /**
     * add recipients
     * @param recipient
     * @throws IOException 
     */
    void addRecipient(CharSequence recipient) throws IOException;

    /**
     * set a list of recipients. Former recipients are dikscarded
     * @param recipients
     * @throws IOException 
     */
    void setRecipients(List<CharSequence> recipients) throws IOException;

    /**
     * recipient is removed
     * @param recipients
     * @throws IOException 
     */
    void removeRecipient(CharSequence recipients) throws IOException;

    /**
     * adds a message
     * @param message
     * @throws IOException 
     */
    void add(CharSequence message) throws IOException;
}
