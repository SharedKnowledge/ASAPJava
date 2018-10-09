
import java.io.InputStream;
import java.io.OutputStream;
import net.sharksystem.asp3.ASP3Engine;

/**
 *
 * @author thsc
 */
public class ASP3EngineThread extends Thread {

    private final ASP3Engine engine;
    private final InputStream is;
    private final OutputStream os;
    
    ASP3EngineThread(ASP3Engine engine, InputStream is, OutputStream os) {
        this.engine = engine;
        this.is = is;
        this.os = os;
    }

    @Override
    public void run() {
        this.engine.handleConnection(is, os);
    }
}
