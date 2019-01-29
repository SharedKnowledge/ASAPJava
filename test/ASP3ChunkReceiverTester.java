
import net.sharksystem.aasp.ASP3ReceivedChunkListener;

/**
 *
 * @author thsc
 */
public class ASP3ChunkReceiverTester implements ASP3ReceivedChunkListener {
    private String sender = null;
    private String uri = null;
    private int era;

    @Override
    public void chunkReceived(String sender, String uri, int era) {
        System.out.println("ChunkReceiverTester.chunkReceived called: (sender/uri/era) " + 
                sender +
                " / " + 
                uri +
                " / " + 
                era);
        this.sender = sender;
        this.uri = uri;
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
