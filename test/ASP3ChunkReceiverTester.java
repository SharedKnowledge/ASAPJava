
import net.sharksystem.asp3.ASP3ReceivedChunkListener;

/**
 *
 * @author thsc
 */
public class ASP3ChunkReceiverTester implements ASP3ReceivedChunkListener {
    private String sender = null;
    private String uri = null;
    private int era;

    @Override
    public void chunkReceived(String sender, String Uri, int era) {
        this.sender = sender;
        this.uri = Uri;
        this.era = era;
    }
    
    public boolean chunkReceived() {
        return this.sender != null;
    }

    String getSender() {
        return this.sender;
    }
    
    String getUri() {
        return this.uri;
    }
    
    int getEra() {
        return this.era;
    }
}
