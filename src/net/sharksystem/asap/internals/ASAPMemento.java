package net.sharksystem.asap.internals;

import java.io.IOException;

/**
 * The memento for the engine.
 *
 * @author thsc
 */
interface ASAPMemento {
    public void save(ASAPEngine engine) throws IOException;
}
