package net.sharksystem.asp3;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
public interface ASP3Writer {
    
    public void addRecipient(CharSequence urlTarget, CharSequence recipient) throws IOException;
    public void setRecipients(CharSequence urlTarget, List<CharSequence> recipients) throws IOException;
    public void removeRecipient(CharSequence urlTarget, CharSequence recipients) throws IOException;
    
    public void add(CharSequence urlTarget, CharSequence message) throws IOException;
    
    public void newEra();
    
    public int getOldestEra();
    public int getEra();
    public int getNextEra(int era);
}
