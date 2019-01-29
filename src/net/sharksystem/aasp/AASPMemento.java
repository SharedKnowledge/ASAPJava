package net.sharksystem.aasp;

import java.io.IOException;

/**
 * The memento for the engine.
 *
 * @author thsc
 */
interface AASPMemento {
    public void save(AASPEngine engine) throws IOException;
}
