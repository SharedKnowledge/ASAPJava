package net.sharksystem.asp3;

/**
 *
 * @author thsc
 */
public interface ASP3Reader {
    public void read(String urlTarget, String message);

    public void read(String urlTarget, String peer, String message);
}
