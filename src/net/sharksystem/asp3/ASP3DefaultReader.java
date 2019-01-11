package net.sharksystem.asp3;

/**
 *
 * @author thsc
 */
class ASP3DefaultReader implements ASP3Reader {

    private String sender;
    private int era;
    private ASP3Storage storage;
    private ASP3Engine engine;

    public ASP3DefaultReader() {
    }

    public void newSender(String sender) {
        this.sender = sender;
    }
    
    @Override
    public void read(String urlTarget, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setEngine(ASP3Engine engine) {
        this.engine = engine;
    }
    
}
