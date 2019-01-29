
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sharksystem.aasp.ASP3Engine;
import net.sharksystem.aasp.ASP3ReceivedChunkListener;

/**
 *
 * @author thsc
 */
public class ASP3EngineThread extends Thread {

    private final ASP3Engine engine;
    private final InputStream is;
    private final OutputStream os;
    private ASP3ReceivedChunkListener listener;
    
    ASP3EngineThread(ASP3Engine engine, InputStream is, 
            OutputStream os, ASP3ReceivedChunkListener listener) {
        
        this.engine = engine;
        this.is = is;
        this.os = os;
        this.listener = listener;
    }

    @Override
    public void run() {
            this.engine.handleConnection(this.is, this.os, this.listener);
    }
}
