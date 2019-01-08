package helper;

import java.util.ArrayList;
import java.util.List;
import net.sharksystem.asp3.ASP3Reader;

/**
 *
 * @author thsc
 */
public class TestReader implements ASP3Reader {

    private final String ownerName;
    List<String> messages = new ArrayList<>();
    
    public TestReader(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public void read(String urlTarget, String message) {
        this.messages.add(message);
        
        // logging
        StringBuilder b = new StringBuilder();
        
        b.append("TestReader (");
        b.append(this.ownerName).append(") got: ");
        b.append("url: ").append(urlTarget);
        b.append(" / message: ").append(message);
        b.append("\n");
        
        System.out.println(b.toString());
    }
    
    public List<String> getMessages() {
        return this.messages;
    }

    @Override
    public void read(String urlTarget, String peer, String message) {
        this.read(urlTarget, message);
    }
    
}
